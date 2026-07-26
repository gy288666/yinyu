package com.yinyu.job;

import com.yinyu.common.constant.RedisKeys;
import com.yinyu.entity.Song;
import com.yinyu.mapper.PlayStatDailyMapper;
import com.yinyu.mapper.SongMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 播放计数回写任务：每 5 分钟将 Redis 中 play:count:{songId} 的增量
 * 回写 song.play_count 与 play_stat_daily 当日统计
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlayCountSyncJob {

    private final StringRedisTemplate redis;
    private final SongMapper songMapper;
    private final PlayStatDailyMapper playStatDailyMapper;

    @Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 60 * 1000)
    public void syncPlayCount() {
        int synced = 0;
        try (Cursor<String> cursor = redis.scan(
                ScanOptions.scanOptions().match(RedisKeys.PLAY_COUNT_PREFIX + "*").count(200).build())) {
            LocalDate today = LocalDate.now();
            while (cursor.hasNext()) {
                String key = cursor.next();
                // GETDEL 原子取走增量，落库失败时回补
                String value = redis.opsForValue().getAndDelete(key);
                if (value == null) {
                    continue;
                }
                long delta;
                Long songId;
                try {
                    delta = Long.parseLong(value);
                    songId = Long.valueOf(key.substring(RedisKeys.PLAY_COUNT_PREFIX.length()));
                } catch (NumberFormatException e) {
                    continue;
                }
                if (delta <= 0) {
                    continue;
                }
                try {
                    Song song = songMapper.selectById(songId);
                    if (song != null) {
                        song.setPlayCount((song.getPlayCount() == null ? 0 : song.getPlayCount()) + delta);
                        songMapper.updateById(song);
                    }
                    playStatDailyMapper.upsertPlayCount(songId, today, delta);
                    synced++;
                } catch (Exception e) {
                    // 回补计数，等待下轮重试
                    redis.opsForValue().increment(key, delta);
                    log.warn("播放计数回写失败 songId={}: {}", songId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("播放计数回写任务异常: {}", e.getMessage());
        }
        if (synced > 0) {
            log.info("播放计数回写完成，共 {} 首歌曲", synced);
        }
    }
}
