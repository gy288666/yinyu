package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;
import com.yinyu.entity.RankSnapshot;
import com.yinyu.entity.Song;
import com.yinyu.mapper.RankSnapshotMapper;
import com.yinyu.mapper.SongMapper;
import com.yinyu.vo.SongVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 排行榜：读 rank_snapshot 最新快照（api.md 9.x）
 * 榜单类型：1-HOT 热歌 2-NEW 新歌 3-ORIGINAL 原创 4-SOAR 飙升
 */
@Service
@RequiredArgsConstructor
public class RankService {

    private static final Map<String, Integer> TYPE_CODE = Map.of(
            "HOT", 1, "NEW", 2, "ORIGINAL", 3, "SOAR", 4);
    private static final Map<String, String> TYPE_NAME = Map.of(
            "HOT", "热歌榜", "NEW", "新歌榜", "ORIGINAL", "原创榜", "SOAR", "飙升榜");

    private final RankSnapshotMapper rankSnapshotMapper;
    private final SongMapper songMapper;
    private final SongAssembler songAssembler;

    /** 榜单列表：四个榜单各带前 3（api.md 9.1） */
    public List<Map<String, Object>> overview() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String type : List.of("HOT", "NEW", "ORIGINAL", "SOAR")) {
            Map<String, Object> data = detail(type, 3, true);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", type);
            item.put("name", TYPE_NAME.get(type));
            item.put("updateTime", data.get("updateTime"));
            item.put("top3", ((List<?>) data.get("songs")).stream()
                    .map(o -> ((Map<?, ?>) o).get("song")).toList());
            result.add(item);
        }
        return result;
    }

    /** 榜单详情（api.md 9.2） */
    public Map<String, Object> detail(String type, int limit, boolean lenient) {
        Integer code = TYPE_CODE.get(type);
        if (code == null) {
            if (lenient) {
                return Map.of("songs", List.of());
            }
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: type 须为 HOT/NEW/ORIGINAL/SOAR");
        }
        limit = Math.min(Math.max(limit, 1), 100);
        // 最新快照日期
        RankSnapshot latest = rankSnapshotMapper.selectOne(new LambdaQueryWrapper<RankSnapshot>()
                .eq(RankSnapshot::getRankType, code)
                .orderByDesc(RankSnapshot::getStatDate).last("LIMIT 1"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", type);
        data.put("name", TYPE_NAME.get(type));
        if (latest == null) {
            data.put("updateTime", null);
            data.put("songs", List.of());
            return data;
        }
        LocalDate date = latest.getStatDate();
        List<RankSnapshot> rows = rankSnapshotMapper.selectList(new LambdaQueryWrapper<RankSnapshot>()
                .eq(RankSnapshot::getRankType, code).eq(RankSnapshot::getStatDate, date)
                .orderByAsc(RankSnapshot::getRankNo).last("LIMIT " + limit));
        // 上一期快照用于计算趋势
        RankSnapshot prevAny = rankSnapshotMapper.selectOne(new LambdaQueryWrapper<RankSnapshot>()
                .eq(RankSnapshot::getRankType, code).lt(RankSnapshot::getStatDate, date)
                .orderByDesc(RankSnapshot::getStatDate).last("LIMIT 1"));
        Map<Long, Integer> prevRanks = prevAny == null ? Map.of()
                : rankSnapshotMapper.selectList(new LambdaQueryWrapper<RankSnapshot>()
                        .eq(RankSnapshot::getRankType, code)
                        .eq(RankSnapshot::getStatDate, prevAny.getStatDate()))
                .stream().collect(Collectors.toMap(RankSnapshot::getSongId, RankSnapshot::getRankNo));

        List<Long> songIds = rows.stream().map(RankSnapshot::getSongId).toList();
        Map<Long, SongVO> songMap = songIds.isEmpty() ? Map.of()
                : songAssembler.toVOs(songMapper.selectBatchIds(songIds)).stream()
                        .collect(Collectors.toMap(SongVO::getId, Function.identity()));

        List<Map<String, Object>> songs = new ArrayList<>();
        for (RankSnapshot row : rows) {
            SongVO song = songMap.get(row.getSongId());
            if (song == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rankNo", row.getRankNo());
            Integer prev = prevRanks.get(row.getSongId());
            if (prev == null) {
                item.put("trend", "NEW");
                item.put("trendDelta", 0);
            } else {
                int delta = prev - row.getRankNo();
                item.put("trend", delta > 0 ? "UP" : delta < 0 ? "DOWN" : "FLAT");
                item.put("trendDelta", Math.abs(delta));
            }
            item.put("song", song);
            songs.add(item);
        }
        data.put("updateTime", latest.getCreateTime());
        data.put("songs", songs);
        return data;
    }
}
