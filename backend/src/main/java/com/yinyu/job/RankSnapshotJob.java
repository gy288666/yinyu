package com.yinyu.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yinyu.entity.PlayStatDaily;
import com.yinyu.entity.RankSnapshot;
import com.yinyu.entity.Song;
import com.yinyu.mapper.PlayStatDailyMapper;
import com.yinyu.mapper.RankSnapshotMapper;
import com.yinyu.mapper.SongMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 榜单快照生成任务：每日凌晨 00:30 生成四榜（HOT/NEW/ORIGINAL/SOAR）写入 rank_snapshot；
 * 写入前清理当日旧快照；应用启动时若当日无快照自动补生成一次。
 * 榜单规则：
 *  1-热歌榜：累计播放量 TOP50
 *  2-新歌榜：近 30 天上架按发布时间倒序 TOP50
 *  3-原创榜：is_original=1 按播放量 TOP50
 *  4-飙升榜：近两日 play_stat_daily 日增量 TOP50
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankSnapshotJob {

    private static final int RANK_SIZE = 50;

    private final SongMapper songMapper;
    private final RankSnapshotMapper rankSnapshotMapper;
    private final PlayStatDailyMapper playStatDailyMapper;

    /** 每日 00:30 生成当日快照 */
    @Scheduled(cron = "0 30 0 * * *")
    public void dailyGenerate() {
        try {
            generate(LocalDate.now());
        } catch (Exception e) {
            log.error("榜单快照生成失败", e);
        }
    }

    /** 启动补偿：当日无快照则生成 */
    @EventListener(ApplicationReadyEvent.class)
    public void generateOnStartupIfMissing() {
        try {
            Long count = rankSnapshotMapper.selectCount(new LambdaQueryWrapper<RankSnapshot>()
                    .eq(RankSnapshot::getStatDate, LocalDate.now()));
            if (count == 0) {
                log.info("当日无榜单快照，启动补生成");
                generate(LocalDate.now());
            }
        } catch (Exception e) {
            log.warn("启动榜单快照补偿失败: {}", e.getMessage());
        }
    }

    /** 生成指定日期快照（后台手动触发亦走此方法） */
    @Transactional
    public Map<String, Integer> generate(LocalDate date) {
        // 清理当日旧快照
        rankSnapshotMapper.delete(new LambdaQueryWrapper<RankSnapshot>()
                .eq(RankSnapshot::getStatDate, date));

        List<Song> online = songMapper.selectList(new LambdaQueryWrapper<Song>()
                .eq(Song::getAuditStatus, 1).eq(Song::getStatus, 1));

        // 1-热歌榜：累计播放量
        List<Song> hot = online.stream()
                .sorted(Comparator.comparingLong((Song s) -> nz(s.getPlayCount())).reversed())
                .limit(RANK_SIZE).toList();
        int hotCount = save(1, date, hot, s -> BigDecimal.valueOf(nz(s.getPlayCount())));

        // 2-新歌榜：近 30 天上架
        LocalDateTime newLimit = date.minusDays(30).atStartOfDay();
        List<Song> newest = online.stream()
                .filter(s -> s.getPublishTime() != null && s.getPublishTime().isAfter(newLimit))
                .sorted(Comparator.comparing(Song::getPublishTime).reversed())
                .limit(RANK_SIZE).toList();
        int newCount = save(2, date, newest, s -> BigDecimal.valueOf(nz(s.getPlayCount())));

        // 3-原创榜
        List<Song> original = online.stream()
                .filter(s -> s.getIsOriginal() != null && s.getIsOriginal() == 1)
                .sorted(Comparator.comparingLong((Song s) -> nz(s.getPlayCount())).reversed())
                .limit(RANK_SIZE).toList();
        int originalCount = save(3, date, original, s -> BigDecimal.valueOf(nz(s.getPlayCount())));

        // 4-飙升榜：近两日日增量合计
        Map<Long, Long> delta = playStatDailyMapper.selectList(new LambdaQueryWrapper<PlayStatDaily>()
                        .ge(PlayStatDaily::getStatDate, date.minusDays(2)))
                .stream().collect(Collectors.groupingBy(PlayStatDaily::getSongId,
                        Collectors.summingLong(s -> s.getPlayCount() == null ? 0 : s.getPlayCount())));
        Map<Long, Song> onlineMap = online.stream().collect(Collectors.toMap(Song::getId, s -> s));
        List<Song> soar = delta.entrySet().stream()
                .filter(e -> e.getValue() > 0 && onlineMap.containsKey(e.getKey()))
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(RANK_SIZE)
                .map(e -> onlineMap.get(e.getKey())).toList();
        int soarCount = save(4, date, soar, s -> BigDecimal.valueOf(delta.getOrDefault(s.getId(), 0L)));

        log.info("榜单快照生成完成 date={} HOT={} NEW={} ORIGINAL={} SOAR={}",
                date, hotCount, newCount, originalCount, soarCount);
        return Map.of("HOT", hotCount, "NEW", newCount, "ORIGINAL", originalCount, "SOAR", soarCount);
    }

    private int save(int rankType, LocalDate date, List<Song> songs,
                     java.util.function.Function<Song, BigDecimal> scoreFn) {
        int rankNo = 1;
        for (Song song : songs) {
            RankSnapshot row = new RankSnapshot();
            row.setRankType(rankType);
            row.setStatDate(date);
            row.setSongId(song.getId());
            row.setRankNo(rankNo++);
            row.setScore(scoreFn.apply(song));
            rankSnapshotMapper.insert(row);
        }
        return songs.size();
    }

    private static long nz(Long v) {
        return v == null ? 0 : v;
    }
}
