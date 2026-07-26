package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 公告表
 */
@Data
@TableName("announcement")
public class Announcement {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公告标题 */
    private String title;

    /** 公告内容（富文本） */
    private String content;

    /** 是否置顶：0-否 1-是 */
    private Integer isTop;

    /** 发布管理员ID -> admin.id */
    private Long adminId;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 状态：0-草稿/下线 1-已发布 */
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
