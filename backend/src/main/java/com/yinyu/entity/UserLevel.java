package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户等级表
 */
@Data
@TableName("user_level")
public class UserLevel {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 等级名称，如 Lv1 初出茅庐 */
    private String name;

    /** 等级数值，从 1 递增 */
    private Integer level;

    /** 达到该等级所需最小成长值 */
    private Long minExp;

    /** 等级图标路径 */
    private String icon;

    /** 等级特权描述 */
    private String privilege;

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
