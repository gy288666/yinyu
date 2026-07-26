package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yinyu.common.constant.RedisKeys;
import com.yinyu.common.result.PageResult;
import com.yinyu.entity.Album;
import com.yinyu.entity.Singer;
import com.yinyu.entity.Song;
import com.yinyu.mapper.AlbumMapper;
import com.yinyu.mapper.SingerMapper;
import com.yinyu.mapper.SongMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 搜索：LIKE 实现（api.md 8.x），关键词热度写入 Redis ZSET
 */
@Service
@RequiredArgsConstructor
public class SearchService {

    private final SongMapper songMapper;
    private final SingerMapper singerMapper;
    private final AlbumMapper albumMapper;
    private final SongAssembler songAssembler;
    private final com.yinyu.service.AlbumService albumService;
    private final StringRedisTemplate redis;

    public PageResult<?> search(String keyword, String type, long pageNum, long pageSize) {
        // 热搜计数
        try {
            redis.opsForZSet().incrementScore(RedisKeys.SEARCH_HOT, keyword, 1);
        } catch (Exception ignored) {
        }
        switch (type == null ? "song" : type) {
            case "singer" -> {
                Page<Singer> page = singerMapper.selectPage(new Page<>(pageNum, pageSize),
                        new LambdaQueryWrapper<Singer>().eq(Singer::getStatus, 1)
                                .like(Singer::getName, keyword).orderByDesc(Singer::getHot));
                return PageResult.from(page, list -> list.stream().map(s -> {
                    Map<String, Object> m = new LinkedHashMap<String, Object>();
                    m.put("id", s.getId());
                    m.put("name", s.getName());
                    m.put("avatar", s.getAvatar());
                    m.put("area", s.getRegion());
                    m.put("type", s.getType());
                    return m;
                }).toList());
            }
            case "album" -> {
                Page<Album> page = albumMapper.selectPage(new Page<>(pageNum, pageSize),
                        new LambdaQueryWrapper<Album>().eq(Album::getStatus, 1)
                                .like(Album::getName, keyword).orderByDesc(Album::getPublishDate));
                return PageResult.from(page, list -> list.stream()
                        .map(a -> albumService.toVO(a, false, null)).toList());
            }
            default -> {
                Page<Song> page = songMapper.selectPage(new Page<>(pageNum, pageSize),
                        new LambdaQueryWrapper<Song>()
                                .eq(Song::getAuditStatus, 1).eq(Song::getStatus, 1)
                                .like(Song::getName, keyword).orderByDesc(Song::getPlayCount));
                return PageResult.from(page, songAssembler::toVOs);
            }
        }
    }

    /** 热搜词（api.md 8.2） */
    public List<String> hot(int limit) {
        Set<String> words = redis.opsForZSet()
                .reverseRange(RedisKeys.SEARCH_HOT, 0, Math.max(limit, 1) - 1);
        return words == null ? List.of() : new ArrayList<>(words);
    }

    /** 搜索联想（api.md 8.3）：前 10 条 */
    public List<Map<String, Object>> suggest(String keyword) {
        List<Map<String, Object>> result = new ArrayList<>();
        songMapper.selectList(new LambdaQueryWrapper<Song>()
                        .eq(Song::getAuditStatus, 1).eq(Song::getStatus, 1)
                        .like(Song::getName, keyword).orderByDesc(Song::getPlayCount).last("LIMIT 5"))
                .forEach(s -> result.add(item("song", s.getId(), s.getName())));
        singerMapper.selectList(new LambdaQueryWrapper<Singer>()
                        .eq(Singer::getStatus, 1)
                        .like(Singer::getName, keyword).orderByDesc(Singer::getHot).last("LIMIT 5"))
                .forEach(s -> result.add(item("singer", s.getId(), s.getName())));
        return result.size() > 10 ? result.subList(0, 10) : result;
    }

    private Map<String, Object> item(String type, Long id, String text) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("id", id);
        m.put("text", text);
        return m;
    }
}
