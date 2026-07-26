package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;
import com.yinyu.common.result.PageResult;
import com.yinyu.entity.Playlist;
import com.yinyu.entity.Song;
import com.yinyu.entity.UserCollectPlaylist;
import com.yinyu.entity.UserLikeSong;
import com.yinyu.mapper.PlaylistMapper;
import com.yinyu.mapper.SongMapper;
import com.yinyu.mapper.UserCollectPlaylistMapper;
import com.yinyu.mapper.UserLikeSongMapper;
import com.yinyu.vo.PlaylistVO;
import com.yinyu.vo.SongVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 喜欢歌曲 / 收藏歌单（api.md 11.x）
 */
@Service
@RequiredArgsConstructor
public class LikeCollectService {

    private final UserLikeSongMapper userLikeSongMapper;
    private final UserCollectPlaylistMapper userCollectPlaylistMapper;
    private final SongMapper songMapper;
    private final PlaylistMapper playlistMapper;
    private final SongAssembler songAssembler;
    private final PlaylistService playlistService;

    /** 喜欢歌曲（幂等，api.md 11.1） */
    @Transactional
    public Map<String, Object> likeSong(Long userId, Long songId) {
        Song song = songMapper.selectById(songId);
        if (song == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        Long exists = userLikeSongMapper.selectCount(new LambdaQueryWrapper<UserLikeSong>()
                .eq(UserLikeSong::getUserId, userId).eq(UserLikeSong::getSongId, songId));
        if (exists == 0) {
            UserLikeSong like = new UserLikeSong();
            like.setUserId(userId);
            like.setSongId(songId);
            userLikeSongMapper.insert(like);
            song.setLikeCount((song.getLikeCount() == null ? 0 : song.getLikeCount()) + 1);
            songMapper.updateById(song);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("liked", true);
        data.put("likeCount", song.getLikeCount());
        return data;
    }

    /** 取消喜欢（api.md 11.2） */
    @Transactional
    public Map<String, Object> unlikeSong(Long userId, Long songId) {
        int deleted = userLikeSongMapper.delete(new LambdaQueryWrapper<UserLikeSong>()
                .eq(UserLikeSong::getUserId, userId).eq(UserLikeSong::getSongId, songId));
        if (deleted > 0) {
            Song song = songMapper.selectById(songId);
            if (song != null && song.getLikeCount() != null && song.getLikeCount() > 0) {
                song.setLikeCount(song.getLikeCount() - 1);
                songMapper.updateById(song);
            }
        }
        return Map.of("liked", false);
    }

    /** 我喜欢的音乐分页（api.md 11.3） */
    public PageResult<SongVO> likedSongs(Long userId, long pageNum, long pageSize) {
        Page<UserLikeSong> page = userLikeSongMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<UserLikeSong>().eq(UserLikeSong::getUserId, userId)
                        .orderByDesc(UserLikeSong::getCreateTime));
        List<Long> songIds = page.getRecords().stream().map(UserLikeSong::getSongId).toList();
        Map<Long, SongVO> map = songIds.isEmpty() ? Map.of()
                : songAssembler.toVOs(songMapper.selectBatchIds(songIds)).stream()
                        .collect(Collectors.toMap(SongVO::getId, Function.identity()));
        List<SongVO> list = songIds.stream().map(map::get).filter(java.util.Objects::nonNull).toList();
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), list);
    }

    /** 收藏歌单（幂等，api.md 11.4） */
    @Transactional
    public void collectPlaylist(Long userId, Long playlistId) {
        Playlist playlist = playlistMapper.selectById(playlistId);
        if (playlist == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        Long exists = userCollectPlaylistMapper.selectCount(new LambdaQueryWrapper<UserCollectPlaylist>()
                .eq(UserCollectPlaylist::getUserId, userId)
                .eq(UserCollectPlaylist::getPlaylistId, playlistId));
        if (exists == 0) {
            UserCollectPlaylist collect = new UserCollectPlaylist();
            collect.setUserId(userId);
            collect.setPlaylistId(playlistId);
            userCollectPlaylistMapper.insert(collect);
            playlist.setCollectCount((playlist.getCollectCount() == null ? 0 : playlist.getCollectCount()) + 1);
            playlistMapper.updateById(playlist);
        }
    }

    /** 取消收藏歌单 */
    @Transactional
    public void uncollectPlaylist(Long userId, Long playlistId) {
        int deleted = userCollectPlaylistMapper.delete(new LambdaQueryWrapper<UserCollectPlaylist>()
                .eq(UserCollectPlaylist::getUserId, userId)
                .eq(UserCollectPlaylist::getPlaylistId, playlistId));
        if (deleted > 0) {
            Playlist playlist = playlistMapper.selectById(playlistId);
            if (playlist != null && playlist.getCollectCount() != null && playlist.getCollectCount() > 0) {
                playlist.setCollectCount(playlist.getCollectCount() - 1);
                playlistMapper.updateById(playlist);
            }
        }
    }

    /** 我的收藏（api.md 11.5）：本期仅 playlist 类型（专辑收藏二期） */
    public PageResult<PlaylistVO> collections(Long userId, String type, long pageNum, long pageSize) {
        if (!"playlist".equals(type)) {
            return PageResult.of(pageNum, pageSize, 0, List.of());
        }
        Page<UserCollectPlaylist> page = userCollectPlaylistMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<UserCollectPlaylist>().eq(UserCollectPlaylist::getUserId, userId)
                        .orderByDesc(UserCollectPlaylist::getCreateTime));
        List<Long> ids = page.getRecords().stream().map(UserCollectPlaylist::getPlaylistId).toList();
        Map<Long, Playlist> playlistMap = ids.isEmpty() ? Map.of()
                : playlistMapper.selectBatchIds(ids).stream()
                        .collect(Collectors.toMap(Playlist::getId, Function.identity()));
        List<PlaylistVO> list = new ArrayList<>();
        for (Long id : ids) {
            Playlist p = playlistMap.get(id);
            if (p == null || p.getStatus() == null || p.getStatus() != 1) {
                // 失效项
                PlaylistVO vo = new PlaylistVO();
                vo.setId(id);
                vo.setTitle(p == null ? "已删除歌单" : p.getName());
                vo.setTags(List.of());
                vo.setInvalid(true);
                list.add(vo);
            } else {
                PlaylistVO vo = playlistService.toVOs(List.of(p)).get(0);
                vo.setInvalid(false);
                list.add(vo);
            }
        }
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), list);
    }
}
