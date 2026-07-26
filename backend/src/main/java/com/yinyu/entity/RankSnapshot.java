package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 榜单快照表
 */
@Data
@TableName("rank_snapshot")
public class RankSnapshot {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 榜单类型：1-热歌榜 2-新歌榜 3-原创榜 4-飙升榜 */
    private Integer rankType;

    /** 榜单日期（生成日） */
    private LocalDate statDate;

    /** 歌曲ID -> song.id */
    private Long songId;

    /** 名次，从 1 开始 */
    private Integer rankNo;

    /** 榜单得分（热度/增速等计算值） */
    private BigDecimal score;

    /** 生成时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
