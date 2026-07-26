package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;
import com.yinyu.common.result.PageResult;
import com.yinyu.entity.Playlist;
import com.yinyu.entity.PlaylistSong;
import com.yinyu.entity.Song;
import com.yinyu.mapper.PlaylistMapper;
import com.yinyu.mapper.PlaylistSongMapper;
import com.yinyu.mapper.SongMapper;
import com.yinyu.util.MinioUtil;
import com.yinyu.util.Params;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台歌单管理：官方歌单 CRUD + 曲目维护（api.md 18.4.11-18.4.15）
 */
@Service
@RequiredArgsConstructor
public class AdminPlaylistService {

    private final PlaylistMapper playlistMapper;
    private final PlaylistSongMapper playlistSongMapper;
    private final SongMapper songMapper;
    private final PlaylistService playlistService;
    private final MinioUtil minioUtil;

    /** 歌单分页（默认官方歌单，official=false 时含用户歌单） */
    public PageResult<Map<String, Object>> page(long pageNum, long pageSize,
                                                String title, Boolean official) {
        LambdaQueryWrapper<Playlist> wrapper = new LambdaQueryWrapper<Playlist>()
                .like(title != null && !title.isEmpty(), Playlist::getName, title)
                .orderByDesc(Playlist::getCreateTime);
        if (official == null || official) {
            wrapper.eq(Playlist::getType, 1);
        }
        Page<Playlist> page = playlistMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.from(page, list -> list.stream().map(this::toVO).toList());
    }

    public Map<String, Object> detail(Long id) {
        Playlist playlist = requirePlaylist(id);
        Map<String, Object> vo = toVO(playlist);
        vo.put("songs", playlistService.songsOf(id));
        return vo;
    }

    /** 新增官方歌单（18.4.12） */
    public Map<String, Object> create(Map<String, Object> body) {
        Playlist playlist = new Playlist();
        playlist.setName(Params.requireStr(body, "title"));
        playlist.setUserId(0L);
        playlist.setType(1);
        playlist.setCover(Params.str(body, "cover"));
        playlist.setIntroduction(Params.str(body, "intro"));
        playlist.setSongCount(0);
        playlist.setPlayCount(0L);
        playlist.setCollectCount(0L);
        playlist.setIsPublic(1);
        playlist.setStatus(1);
        playlistMapper.insert(playlist);
        return toVO(playlist);
    }

    /** 修改官方歌单（18.4.13） */
    public void update(Long id, Map<String, Object> body) {
        Playlist playlist = requirePlaylist(id);
        String title = Params.str(body, "title");
        if (title != null) {
            playlist.setName(title);
        }
        if (body.containsKey("cover")) {
            playlist.setCover(Params.str(body, "cover"));
        }
        if (body.containsKey("intro")) {
            playlist.setIntroduction(Params.str(body, "intro"));
        }
        Boolean enabled = Params.bool(body, "enabled");
        if (enabled != null) {
            playlist.setStatus(enabled ? 1 : 0);
        }
        playlistMapper.updateById(playlist);
    }

    /** 删除官方歌单（18.4.14）：连带清理曲目关系 */
    @Transactional
    public void delete(Long id) {
        requirePlaylist(id);
        playlistSongMapper.delete(new LambdaQueryWrapper<PlaylistSong>()
                .eq(PlaylistSong::getPlaylistId, id));
        playlistMapper.deleteById(id);
    }

    /** 歌单曲目维护（18.4.15）：songIds 有序数组整体替换 */
    @Transactional
    public Map<String, Object> replaceSongs(Long id, List<Long> songIds) {
        Playlist playlist = requirePlaylist(id);
        if (songIds == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: songIds 不能为空");
        }
        List<Long> distinct = songIds.stream().distinct().toList();
        // 校验歌曲存在
        if (!distinct.isEmpty()) {
            List<Long> existing = songMapper.selectBatchIds(distinct).stream().map(Song::getId).toList();
            if (existing.size() != distinct.size()) {
                throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "部分歌曲不存在");
            }
        }
        playlistSongMapper.delete(new LambdaQueryWrapper<PlaylistSong>()
                .eq(PlaylistSong::getPlaylistId, id));
        int sort = 1;
        for (Long songId : distinct) {
            PlaylistSong rel = new PlaylistSong();
            rel.setPlaylistId(id);
            rel.setSongId(songId);
            rel.setSort(sort++);
            playlistSongMapper.insert(rel);
        }
        playlist.setSongCount(distinct.size());
        playlistMapper.updateById(playlist);
        return Map.of("songCount", distinct.size());
    }

    private Playlist requirePlaylist(Long id) {
        Playlist playlist = playlistMapper.selectById(id);
        if (playlist == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        return playlist;
    }

    private Map<String, Object> toVO(Playlist p) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", p.getId());
        vo.put("title", p.getName());
        vo.put("cover", minioUtil.publicImageUrl(p.getCover()));
        vo.put("intro", p.getIntroduction());
        vo.put("official", p.getType() != null && p.getType() == 1);
        vo.put("songCount", p.getSongCount());
        vo.put("playCount", p.getPlayCount());
        vo.put("collectCount", p.getCollectCount());
        vo.put("enabled", p.getStatus() != null && p.getStatus() == 1);
        vo.put("createTime", p.getCreateTime());
        return vo;
    }
}
