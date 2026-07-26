package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;
import com.yinyu.common.result.PageResult;
import com.yinyu.entity.Playlist;
import com.yinyu.entity.PlaylistSong;
import com.yinyu.entity.Song;
import com.yinyu.entity.User;
import com.yinyu.mapper.PlaylistMapper;
import com.yinyu.mapper.PlaylistSongMapper;
import com.yinyu.mapper.SongMapper;
import com.yinyu.mapper.UserMapper;
import com.yinyu.util.MinioUtil;
import com.yinyu.vo.PlaylistDetailVO;
import com.yinyu.vo.PlaylistVO;
import com.yinyu.vo.SongVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 歌单：广场/详情/我的歌单 CRUD/歌曲增删（api.md 6.x）
 */
@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistMapper playlistMapper;
    private final PlaylistSongMapper playlistSongMapper;
    private final SongMapper songMapper;
    private final UserMapper userMapper;
    private final com.yinyu.mapper.UserLikeSongMapper userLikeSongMapper;
    private final SongAssembler songAssembler;
    private final MinioUtil minioUtil;

    /** 歌单广场（api.md 6.1）：公开且正常的歌单 */
    public PageResult<PlaylistVO> square(long pageNum, long pageSize, String tag, String sort) {
        LambdaQueryWrapper<Playlist> wrapper = new LambdaQueryWrapper<Playlist>()
                .eq(Playlist::getIsPublic, 1).eq(Playlist::getStatus, 1);
        // tag 筛选：本期库表无歌单标签关系，按名称模糊近似
        if (tag != null && !tag.isEmpty()) {
            wrapper.like(Playlist::getName, tag);
        }
        if ("latest".equals(sort)) {
            wrapper.orderByDesc(Playlist::getCreateTime);
        } else {
            wrapper.orderByDesc(Playlist::getPlayCount);
        }
        Page<Playlist> page = playlistMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.from(page, this::toVOs);
    }

    /** 热门歌单（api.md 6.2）：官方推荐 + 播放量排序 */
    public List<PlaylistVO> hot(int limit) {
        List<Playlist> list = playlistMapper.selectList(new LambdaQueryWrapper<Playlist>()
                .eq(Playlist::getIsPublic, 1).eq(Playlist::getStatus, 1)
                .orderByDesc(Playlist::getType)
                .orderByDesc(Playlist::getPlayCount)
                .last("LIMIT " + Math.min(Math.max(limit, 1), 50)));
        return toVOs(list);
    }

    /** 歌单详情（api.md 6.3）：私密歌单仅创建者可见 */
    public PlaylistDetailVO detail(Long id, Long userId) {
        Playlist playlist = playlistMapper.selectById(id);
        if (playlist == null || playlist.getStatus() == null || playlist.getStatus() != 1) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        if (playlist.getIsPublic() != null && playlist.getIsPublic() == 0
                && !playlist.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.PRIVATE_RESOURCE, "无权访问该私密资源");
        }
        PlaylistDetailVO vo = new PlaylistDetailVO();
        fill(vo, playlist, resolveCreatorNames(List.of(playlist)));
        vo.setIntro(playlist.getIntroduction());
        vo.setVisibility(playlist.getIsPublic() != null && playlist.getIsPublic() == 0 ? "PRIVATE" : "PUBLIC");
        vo.setCreatorId(playlist.getType() != null && playlist.getType() == 1 ? null : playlist.getUserId());
        vo.setCollectCount(playlist.getCollectCount());
        vo.setCollected(false);
        vo.setSongs(songsOf(id));
        return vo;
    }

    /** 歌单内曲目（按 sort） */
    public List<SongVO> songsOf(Long playlistId) {
        List<PlaylistSong> relations = playlistSongMapper.selectList(
                new LambdaQueryWrapper<PlaylistSong>()
                        .eq(PlaylistSong::getPlaylistId, playlistId)
                        .orderByAsc(PlaylistSong::getSort));
        if (relations.isEmpty()) {
            return List.of();
        }
        List<Long> songIds = relations.stream().map(PlaylistSong::getSongId).toList();
        Map<Long, SongVO> map = songAssembler.toVOs(songMapper.selectBatchIds(songIds))
                .stream().collect(Collectors.toMap(SongVO::getId, Function.identity()));
        return songIds.stream().map(map::get).filter(java.util.Objects::nonNull).toList();
    }

    /** 我的歌单（api.md 6.4）：首位为虚拟"我喜欢的音乐"内置歌单（id=0，曲目见 /api/likes/songs） */
    public List<PlaylistVO> myPlaylists(Long userId) {
        List<PlaylistVO> result = new ArrayList<>();
        PlaylistVO builtin = new PlaylistVO();
        builtin.setId(0L);
        builtin.setTitle("我喜欢的音乐");
        builtin.setTags(List.of());
        builtin.setSongCount(Math.toIntExact(userLikeSongMapper.selectCount(
                new LambdaQueryWrapper<com.yinyu.entity.UserLikeSong>()
                        .eq(com.yinyu.entity.UserLikeSong::getUserId, userId))));
        builtin.setPlayCount(0L);
        builtin.setOfficial(false);
        builtin.setBuiltin(true);
        result.add(builtin);

        List<Playlist> mine = playlistMapper.selectList(new LambdaQueryWrapper<Playlist>()
                .eq(Playlist::getUserId, userId)
                .orderByDesc(Playlist::getCreateTime));
        List<PlaylistVO> vos = toVOs(mine);
        vos.forEach(v -> v.setBuiltin(false));
        result.addAll(vos);
        return result;
    }

    /** 创建歌单（api.md 6.5） */
    public PlaylistVO create(Long userId, String title, String cover, String intro, String visibility) {
        if (title == null || title.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: title 不能为空");
        }
        Playlist p = new Playlist();
        p.setName(title);
        p.setUserId(userId);
        p.setType(0);
        p.setCover(cover);
        p.setIntroduction(intro);
        p.setIsPublic("PRIVATE".equals(visibility) ? 0 : 1);
        p.setSongCount(0);
        p.setPlayCount(0L);
        p.setCollectCount(0L);
        p.setStatus(1);
        playlistMapper.insert(p);
        PlaylistVO vo = toVOs(List.of(p)).get(0);
        vo.setBuiltin(false);
        return vo;
    }

    /** 修改歌单（api.md 6.6）：仅创建者 */
    public void update(Long userId, Long id, String title, String cover, String intro, String visibility) {
        Playlist p = requireOwned(userId, id);
        if (title != null && !title.isBlank()) {
            p.setName(title);
        }
        if (cover != null) {
            p.setCover(cover);
        }
        if (intro != null) {
            p.setIntroduction(intro);
        }
        if (visibility != null) {
            p.setIsPublic("PRIVATE".equals(visibility) ? 0 : 1);
        }
        playlistMapper.updateById(p);
    }

    /** 删除歌单（api.md 6.7）：仅创建者 */
    @Transactional
    public void delete(Long userId, Long id) {
        requireOwned(userId, id);
        playlistMapper.deleteById(id);
        playlistSongMapper.delete(new LambdaQueryWrapper<PlaylistSong>()
                .eq(PlaylistSong::getPlaylistId, id));
    }

    /** 添加歌曲（api.md 6.8）：幂等 */
    @Transactional
    public Map<String, Object> addSong(Long userId, Long playlistId, Long songId) {
        Playlist p = requireOwned(userId, playlistId);
        Song song = songMapper.selectById(songId);
        if (song == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        Long exists = playlistSongMapper.selectCount(new LambdaQueryWrapper<PlaylistSong>()
                .eq(PlaylistSong::getPlaylistId, playlistId).eq(PlaylistSong::getSongId, songId));
        if (exists == 0) {
            PlaylistSong ps = new PlaylistSong();
            ps.setPlaylistId(playlistId);
            ps.setSongId(songId);
            ps.setSort(p.getSongCount() == null ? 0 : p.getSongCount() + 1);
            playlistSongMapper.insert(ps);
            refreshSongCount(p);
        }
        return Map.of("songCount", p.getSongCount());
    }

    /** 移除歌曲（api.md 6.8） */
    @Transactional
    public Map<String, Object> removeSong(Long userId, Long playlistId, Long songId) {
        Playlist p = requireOwned(userId, playlistId);
        playlistSongMapper.delete(new LambdaQueryWrapper<PlaylistSong>()
                .eq(PlaylistSong::getPlaylistId, playlistId).eq(PlaylistSong::getSongId, songId));
        refreshSongCount(p);
        return Map.of("songCount", p.getSongCount());
    }

    private void refreshSongCount(Playlist p) {
        long count = playlistSongMapper.selectCount(new LambdaQueryWrapper<PlaylistSong>()
                .eq(PlaylistSong::getPlaylistId, p.getId()));
        p.setSongCount((int) count);
        playlistMapper.updateById(p);
    }

    private Playlist requireOwned(Long userId, Long id) {
        Playlist p = playlistMapper.selectById(id);
        if (p == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        if (p.getType() != null && p.getType() == 1) {
            throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "官方歌单不可操作");
        }
        if (!p.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无操作权限");
        }
        return p;
    }

    public List<PlaylistVO> toVOs(List<Playlist> list) {
        Map<Long, String> creatorNames = resolveCreatorNames(list);
        return list.stream().map(p -> {
            PlaylistVO vo = new PlaylistVO();
            fill(vo, p, creatorNames);
            return vo;
        }).toList();
    }

    private Map<Long, String> resolveCreatorNames(List<Playlist> list) {
        List<Long> userIds = list.stream()
                .filter(p -> p.getType() == null || p.getType() == 0)
                .map(Playlist::getUserId).filter(id -> id != null && id > 0).distinct().toList();
        Map<Long, String> names = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> names.put(u.getId(), u.getNickname()));
        }
        return names;
    }

    private void fill(PlaylistVO vo, Playlist p, Map<Long, String> creatorNames) {
        boolean official = p.getType() != null && p.getType() == 1;
        vo.setId(p.getId());
        vo.setTitle(p.getName());
        vo.setCover(minioUtil.publicImageUrl(p.getCover()));
        vo.setTags(List.of());
        vo.setPlayCount(p.getPlayCount());
        vo.setSongCount(p.getSongCount());
        vo.setCreatorName(official ? "音域运营" : creatorNames.getOrDefault(p.getUserId(), "未知用户"));
        vo.setOfficial(official);
    }
}
