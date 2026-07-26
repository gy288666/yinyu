package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.PlayStatDaily;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/** 歌曲每日统计表 Mapper */
@Mapper
public interface PlayStatDailyMapper extends BaseMapper<PlayStatDaily> {

    /** 播放量按 歌曲+日期 累加（Redis 计数定时回写用） */
    @Insert("INSERT INTO play_stat_daily (song_id, stat_date, play_count) " +
            "VALUES (#{songId}, #{statDate}, #{delta}) " +
            "ON DUPLICATE KEY UPDATE play_count = play_count + #{delta}")
    int upsertPlayCount(@Param("songId") Long songId,
                        @Param("statDate") LocalDate statDate,
                        @Param("delta") long delta);
}
