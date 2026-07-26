package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 管理员-角色关联表
 */
@Data
@TableName("admin_role")
public class AdminRole {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 管理员ID -> admin.id */
    private Long adminId;

    /** 角色ID -> role.id */
    private Long roleId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
