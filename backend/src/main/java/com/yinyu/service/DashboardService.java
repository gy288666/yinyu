package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yinyu.common.constant.RedisKeys;
import com.yinyu.entity.OrderInfo;
import com.yinyu.entity.PlayStatDaily;
import com.yinyu.entity.Song;
import com.yinyu.entity.User;
import com.yinyu.mapper.OrderInfoMapper;
import com.yinyu.mapper.PlayStatDailyMapper;
import com.yinyu.mapper.SongMapper;
import com.yinyu.mapper.UserMapper;
import com.yinyu.vo.SongVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 后台数据看板（api.md 18.1）
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SongMapper songMapper;
    private final UserMapper userMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final PlayStatDailyMapper playStatDailyMapper;
    private final SongAssembler songAssembler;
    private final StringRedisTemplate redis;

    /** 看板汇总（18.1.1）：音乐总数/用户总数/今日播放量[Redis]/付费会员数/收益合计 */
    public Map<String, Object> summary() {
        long songTotal = songMapper.selectCount(
                new LambdaQueryWrapper<Song>().eq(Song::getAuditStatus, 1));
        long userTotal = userMapper.selectCount(null);
        long todayPlayCount = todayPlayCount();
        long vipCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getIsVip, 1).gt(User::getVipExpireTime, LocalDateTime.now()));
        BigDecimal revenue = orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>()
                        .eq(OrderInfo::getStatus, 1))
                .stream().map(OrderInfo::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 环比：以昨日播放量粗略计算，其余指标本期置 0
        long yesterdayPlay = dailyPlaySum(LocalDate.now().minusDays(1));
        double playCompare = yesterdayPlay == 0 ? 0
                : (double) (todayPlayCount - yesterdayPlay) / yesterdayPlay;

        Map<String, Object> compare = new LinkedHashMap<>();
        compare.put("songTotal", 0);
        compare.put("userTotal", 0);
        compare.put("todayPlayCount", BigDecimal.valueOf(playCompare).setScale(3, RoundingMode.HALF_UP));
        compare.put("vipCount", 0);
        compare.put("revenue", 0);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("songTotal", songTotal);
        data.put("userTotal", userTotal);
        data.put("todayPlayCount", todayPlayCount);
        data.put("vipCount", vipCount);
        data.put("revenue", revenue.setScale(2, RoundingMode.HALF_UP).toPlainString());
        data.put("compare", compare);
        return data;
    }

    /** 今日实时播放量：日榜 ZSET 分数合计 */
    public long todayPlayCount() {
        Set<ZSetOperations.TypedTuple<String>> tuples = redis.opsForZSet()
                .rangeWithScores(RedisKeys.RANK_DAY_PREFIX + LocalDate.now().format(DAY), 0, -1);
        if (tuples == null) {
            return 0;
        }
        return tuples.stream().mapToLong(t -> t.getScore() == null ? 0 : t.getScore().longValue()).sum();
    }

    private long dailyPlaySum(LocalDate date) {
        return playStatDailyMapper.selectList(new LambdaQueryWrapper<PlayStatDaily>()
                        .eq(PlayStatDaily::getStatDate, date))
                .stream().mapToLong(s -> s.getPlayCount() == null ? 0 : s.getPlayCount()).sum();
    }

    /** 播放量趋势（18.1.2）：play_stat_daily 聚合，当日取 Redis 实时值 */
    public List<Map<String, Object>> playTrend(int days) {
        days = days == 30 ? 30 : 7;
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(days - 1);
        Map<LocalDate, Long> byDate = playStatDailyMapper.selectList(
                        new LambdaQueryWrapper<PlayStatDaily>().ge(PlayStatDaily::getStatDate, start))
                .stream().collect(Collectors.groupingBy(PlayStatDaily::getStatDate,
                        Collectors.summingLong(s -> s.getPlayCount() == null ? 0 : s.getPlayCount())));
        List<Map<String, Object>> result = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", d.toString());
            item.put("count", d.equals(today) ? todayPlayCount() : byDate.getOrDefault(d, 0L));
            result.add(item);
        }
        return result;
    }

    /** 用户来源渠道分布（18.1.3）：user.register_channel 聚合 */
    public List<Map<String, Object>> userChannels() {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.select("register_channel AS channel", "COUNT(*) AS cnt")
                .eq("deleted", 0)
                .groupBy("register_channel");
        List<Map<String, Object>> rows = userMapper.selectMaps(wrapper);
        long total = rows.stream().mapToLong(r -> ((Number) r.get("cnt")).longValue()).sum();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            long count = ((Number) row.get("cnt")).longValue();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("channel", row.get("channel"));
            item.put("count", count);
            item.put("ratio", total == 0 ? 0 : BigDecimal.valueOf((double) count / total)
                    .setScale(2, RoundingMode.HALF_UP));
            result.add(item);
        }
        return result;
    }

    /** 热门歌曲 TOP5（18.1.5）：今日日榜 ZSET 前 5 */
    public List<Map<String, Object>> hotSongs() {
        Set<ZSetOperations.TypedTuple<String>> tuples = redis.opsForZSet().reverseRangeWithScores(
                RedisKeys.RANK_DAY_PREFIX + LocalDate.now().format(DAY), 0, 4);
        List<Map<String, Object>> result = new ArrayList<>();
        if (tuples == null || tuples.isEmpty()) {
            // 日榜为空时回退到累计播放量前 5
            List<Song> songs = songMapper.selectList(new LambdaQueryWrapper<Song>()
                    .eq(Song::getAuditStatus, 1).eq(Song::getStatus, 1)
                    .orderByDesc(Song::getPlayCount).last("LIMIT 5"));
            for (SongVO vo : songAssembler.toVOs(songs)) {
                result.add(hotItem(vo.getId(), vo.getName(), vo.getSingerName(), 0L));
            }
            return result;
        }
        List<Long> ids = tuples.stream()
                .map(t -> Long.valueOf(String.valueOf(t.getValue()))).toList();
        Map<Long, SongVO> songMap = songAssembler.toVOs(songMapper.selectBatchIds(ids)).stream()
                .collect(Collectors.toMap(SongVO::getId, Function.identity()));
        for (ZSetOperations.TypedTuple<String> t : tuples) {
            Long id = Long.valueOf(String.valueOf(t.getValue()));
            SongVO vo = songMap.get(id);
            if (vo != null) {
                result.add(hotItem(id, vo.getName(), vo.getSingerName(),
                        t.getScore() == null ? 0 : t.getScore().longValue()));
            }
        }
        return result;
    }

    private Map<String, Object> hotItem(Long songId, String name, String singerName, Long todayPlayCount) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("songId", songId);
        item.put("name", name);
        item.put("singerName", singerName);
        item.put("todayPlayCount", todayPlayCount);
        return item;
    }
}
