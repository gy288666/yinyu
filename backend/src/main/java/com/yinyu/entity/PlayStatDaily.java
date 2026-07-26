package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 歌曲每日统计表
 */
@Data
@TableName("play_stat_daily")
public class PlayStatDaily {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 歌曲ID -> song.id */
    private Long songId;

    /** 统计日期 */
    private LocalDate statDate;

    /** 当日播放量 */
    private Long playCount;

    /** 当日新增喜欢数 */
    private Long likeCount;

    /** 当日新增收藏数 */
    private Long collectCount;

    /** 当日下载量 */
    private Long downloadCount;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
