package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yinyu.common.result.PageResult;
import com.yinyu.entity.RecentPlay;
import com.yinyu.entity.Song;
import com.yinyu.mapper.RecentPlayMapper;
import com.yinyu.mapper.SongMapper;
import com.yinyu.vo.SongVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 最近播放：由播放地址接口自动写入（api.md 12），最多保留 200 条
 */
@Service
@RequiredArgsConstructor
public class RecentPlayService {

    private static final int MAX_KEEP = 200;

    private final RecentPlayMapper recentPlayMapper;
    private final SongMapper songMapper;
    private final SongAssembler songAssembler;

    /** 记录一次播放（同曲目去重，更新时间与次数） */
    public void record(Long userId, Long songId) {
        RecentPlay exist = recentPlayMapper.selectOne(new LambdaQueryWrapper<RecentPlay>()
                .eq(RecentPlay::getUserId, userId).eq(RecentPlay::getSongId, songId));
        if (exist != null) {
            exist.setPlayTime(LocalDateTime.now());
            exist.setPlayCount(exist.getPlayCount() == null ? 1 : exist.getPlayCount() + 1);
            recentPlayMapper.updateById(exist);
            return;
        }
        RecentPlay rp = new RecentPlay();
        rp.setUserId(userId);
        rp.setSongId(songId);
        rp.setPlayTime(LocalDateTime.now());
        rp.setPlayCount(1);
        recentPlayMapper.insert(rp);
        // 超出保留上限时裁剪最旧记录
        Long count = recentPlayMapper.selectCount(
                new LambdaQueryWrapper<RecentPlay>().eq(RecentPlay::getUserId, userId));
        if (count != null && count > MAX_KEEP) {
            List<RecentPlay> overflow = recentPlayMapper.selectList(new LambdaQueryWrapper<RecentPlay>()
                    .eq(RecentPlay::getUserId, userId)
                    .orderByAsc(RecentPlay::getPlayTime)
                    .last("LIMIT " + (count - MAX_KEEP)));
            if (!overflow.isEmpty()) {
                recentPlayMapper.deleteBatchIds(overflow.stream().map(RecentPlay::getId).toList());
            }
        }
    }

    /** 最近播放分页（api.md 12.1） */
    public PageResult<Map<String, Object>> page(Long userId, long pageNum, long pageSize) {
        Page<RecentPlay> page = recentPlayMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<RecentPlay>().eq(RecentPlay::getUserId, userId)
                        .orderByDesc(RecentPlay::getPlayTime));
        List<Long> songIds = page.getRecords().stream().map(RecentPlay::getSongId).toList();
        Map<Long, SongVO> songMap = songIds.isEmpty() ? Map.of()
                : songAssembler.toVOs(songMapper.selectBatchIds(songIds).stream().toList())
                        .stream().collect(Collectors.toMap(SongVO::getId, Function.identity()));
        List<Map<String, Object>> list = new ArrayList<>();
        for (RecentPlay rp : page.getRecords()) {
            SongVO song = songMap.get(rp.getSongId());
            if (song == null) {
                continue; // 歌曲已删除
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("song", song);
            item.put("playAt", rp.getPlayTime());
            list.add(item);
        }
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), list);
    }

    /** 清空最近播放（api.md 12.2） */
    public void clear(Long userId) {
        recentPlayMapper.delete(new LambdaQueryWrapper<RecentPlay>().eq(RecentPlay::getUserId, userId));
    }
}
