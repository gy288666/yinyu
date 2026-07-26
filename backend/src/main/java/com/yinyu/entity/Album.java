package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 专辑表
 */
@Data
@TableName("album")
public class Album {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 专辑名称 */
    private String name;

    /** 主歌手ID -> singer.id */
    private Long singerId;

    /** 专辑封面路径 */
    private String cover;

    /** 发行日期 */
    private LocalDate publishDate;

    /** 发行公司 */
    private String company;

    /** 专辑简介 */
    private String introduction;

    /** 收录歌曲数（冗余计数） */
    private Integer songCount;

    /** 状态：0-下架 1-上架 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
