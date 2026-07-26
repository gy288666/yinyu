package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 角色-权限关联表
 */
@Data
@TableName("role_permission")
public class RolePermission {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色ID -> role.id */
    private Long roleId;

    /** 权限ID -> permission.id */
    private Long permissionId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
