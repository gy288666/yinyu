package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 权限表
 */
@Data
@TableName("permission")
public class Permission {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父权限ID，0=顶级 */
    private Long parentId;

    /** 权限名称 */
    private String name;

    /** 权限编码，如 music:song:audit */
    private String code;

    /** 类型：1-菜单 2-按钮 3-接口 */
    private Integer type;

    /** 前端路由或接口路径 */
    private String path;

    /** 菜单图标 */
    private String icon;

    /** 排序值 */
    private Integer sort;

    /** 状态：0-禁用 1-启用 */
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
