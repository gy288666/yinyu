package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.constant.RedisKeys;
import com.yinyu.common.exception.BizException;
import com.yinyu.entity.Playlist;
import com.yinyu.entity.RecentPlay;
import com.yinyu.entity.Song;
import com.yinyu.entity.SongTag;
import com.yinyu.entity.UserLikeSong;
import com.yinyu.mapper.PlaylistMapper;
import com.yinyu.mapper.RecentPlayMapper;
import com.yinyu.mapper.SongMapper;
import com.yinyu.mapper.SongTagMapper;
import com.yinyu.mapper.UserLikeSongMapper;
import com.yinyu.vo.SongVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 推荐 / 私人FM（api.md 10.1-10.5）：标签偏好 + 热度 + 随机的伪推荐实现
 */
@Service
@RequiredArgsConstructor
public class RecommendService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int DAILY_SIZE = 30;

    private final SongMapper songMapper;
    private final SongTagMapper songTagMapper;
    private final UserLikeSongMapper userLikeSongMapper;
    private final RecentPlayMapper recentPlayMapper;
    private final PlaylistMapper playlistMapper;
    private final SongAssembler songAssembler;
    private final PlaylistService playlistService;
    private final StringRedisTemplate redis;

    private List<Song> onlineSongs(int limit) {
        return songMapper.selectList(new LambdaQueryWrapper<Song>()
                .eq(Song::getAuditStatus, 1).eq(Song::getStatus, 1)
                .orderByDesc(Song::getPlayCount)
                .last("LIMIT " + limit));
    }

    /** 每日推荐（10.1）：标签偏好加权 + 热度 + 日固定随机种子，当日 Redis 缓存 */
    public Map<String, Object> daily(Long userId) {
        LocalDate today = LocalDate.now();
        String cacheKey = RedisKeys.REC_DAILY_PREFIX + today.format(DAY) + ":"
                + (userId == null ? "guest" : userId);
        String cached = redis.opsForValue().get(cacheKey);
        List<SongVO> songs;
        if (cached != null && !cached.isEmpty()) {
            List<Long> ids = Arrays.stream(cached.split(",")).map(Long::valueOf).toList();
            Map<Long, SongVO> map = songAssembler.toVOs(songMapper.selectBatchIds(ids)).stream()
                    .collect(Collectors.toMap(SongVO::getId, v -> v));
            songs = ids.stream().map(map::get).filter(java.util.Objects::nonNull).toList();
        } else {
            songs = computeDaily(userId, today);
            if (!songs.isEmpty()) {
                redis.opsForValue().set(cacheKey,
                        songs.stream().map(v -> String.valueOf(v.getId()))
                                .collect(Collectors.joining(",")),
                        Duration.ofHours(26));
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("date", today.toString());
        data.put("songs", songs);
        return data;
    }

    private List<SongVO> computeDaily(Long userId, LocalDate date) {
        List<Song> pool = onlineSongs(300);
        if (pool.isEmpty()) {
            return List.of();
        }
        // 标签偏好：用户喜欢过的歌曲标签集合
        Set<Long> preferredTags = new HashSet<>();
        if (userId != null) {
            List<Long> likedSongIds = userLikeSongMapper.selectList(new LambdaQueryWrapper<UserLikeSong>()
                            .eq(UserLikeSong::getUserId, userId))
                    .stream().map(UserLikeSong::getSongId).toList();
            if (!likedSongIds.isEmpty()) {
                preferredTags = songTagMapper.selectList(new LambdaQueryWrapper<SongTag>()
                                .in(SongTag::getSongId, likedSongIds))
                        .stream().map(SongTag::getTagId).collect(Collectors.toSet());
            }
        }
        Map<Long, Set<Long>> songTags = songTagMapper.selectList(new LambdaQueryWrapper<SongTag>()
                        .in(SongTag::getSongId, pool.stream().map(Song::getId).toList()))
                .stream().collect(Collectors.groupingBy(SongTag::getSongId,
                        Collectors.mapping(SongTag::getTagId, Collectors.toSet())));

        long maxPlay = pool.stream().mapToLong(s -> s.getPlayCount() == null ? 0 : s.getPlayCount())
                .max().orElse(1);
        Random random = new Random(date.toEpochDay() * 31 + (userId == null ? 0 : userId));
        Set<Long> finalTags = preferredTags;
        List<Song> ranked = pool.stream()
                .sorted(java.util.Comparator.comparingDouble((Song s) -> {
                    double hot = (s.getPlayCount() == null ? 0 : s.getPlayCount()) / (double) Math.max(maxPlay, 1);
                    long tagHit = songTags.getOrDefault(s.getId(), Set.of()).stream()
                            .filter(finalTags::contains).count();
                    double score = hot * 0.4 + Math.min(tagHit, 3) * 0.2 + random.nextDouble() * 0.4;
                    return -score;
                })).limit(DAILY_SIZE).toList();
        return songAssembler.toVOs(ranked);
    }

    /** 为你推荐（10.2）：热门池随机取样 */
    public List<SongVO> songs(int limit) {
        limit = Math.min(Math.max(limit, 1), 50);
        List<Song> pool = new ArrayList<>(onlineSongs(100));
        java.util.Collections.shuffle(pool, ThreadLocalRandom.current());
        return songAssembler.toVOs(pool.subList(0, Math.min(limit, pool.size())));
    }

    /** 推荐歌单（10.3）：官方优先 + 播放量 + 随机打散 */
    public List<com.yinyu.vo.PlaylistVO> playlists(int limit) {
        limit = Math.min(Math.max(limit, 1), 20);
        List<Playlist> pool = playlistMapper.selectList(new LambdaQueryWrapper<Playlist>()
                .eq(Playlist::getIsPublic, 1).eq(Playlist::getStatus, 1)
                .orderByDesc(Playlist::getType)
                .orderByDesc(Playlist::getPlayCount)
                .last("LIMIT " + (limit * 3)));
        java.util.Collections.shuffle(pool, ThreadLocalRandom.current());
        return playlistService.toVOs(pool.subList(0, Math.min(limit, pool.size())));
    }

    // ---------------- 私人FM ----------------

    /** FM 下一曲（10.4）：随机一首未听过、7 天内未被标记不喜欢的已上架歌曲 */
    public SongVO fmNext(Long userId) {
        Set<Long> exclude = new HashSet<>(dislikedSongIds(userId));
        recentPlayMapper.selectList(new LambdaQueryWrapper<RecentPlay>()
                        .eq(RecentPlay::getUserId, userId))
                .forEach(r -> exclude.add(r.getSongId()));
        List<Song> pool = onlineSongs(500).stream()
                .filter(s -> !exclude.contains(s.getId())).toList();
        if (pool.isEmpty()) {
            // 全部听过时仅排除"不喜欢"
            Set<Long> disliked = dislikedSongIds(userId);
            pool = onlineSongs(500).stream().filter(s -> !disliked.contains(s.getId())).toList();
        }
        if (pool.isEmpty()) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "暂无可推荐曲目");
        }
        Song pick = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        return songAssembler.toVO(pick);
    }

    /** FM 不喜欢（10.5）：7 天内不再出现 */
    public void fmDislike(Long userId, Long songId) {
        if (songId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: songId 不能为空");
        }
        String key = RedisKeys.FM_DISLIKE_PREFIX + userId;
        long expireAt = LocalDateTime.now().plusDays(7)
                .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        redis.opsForZSet().add(key, String.valueOf(songId), expireAt);
        redis.expire(key, Duration.ofDays(8));
    }

    private Set<Long> dislikedSongIds(Long userId) {
        String key = RedisKeys.FM_DISLIKE_PREFIX + userId;
        long now = System.currentTimeMillis();
        // 清理已过期（score < now）
        redis.opsForZSet().removeRangeByScore(key, 0, now);
        Set<String> members = redis.opsForZSet().rangeByScore(key, now, Double.MAX_VALUE);
        return members == null ? Set.of()
                : members.stream().map(Long::valueOf).collect(Collectors.toSet());
    }
}
