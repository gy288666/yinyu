package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 电台表
 */
@Data
@TableName("radio")
public class Radio {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 电台名称 */
    private String name;

    /** 电台封面路径 */
    private String cover;

    /** 电台简介 */
    private String introduction;

    /** 播放量（冗余计数） */
    private Long playCount;

    /** 排序值 */
    private Integer sort;

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
