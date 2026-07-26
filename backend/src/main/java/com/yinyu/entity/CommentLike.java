package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 评论点赞表
 */
@Data
@TableName("comment_like")
public class CommentLike {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID -> user.id */
    private Long userId;

    /** 评论ID -> comment.id */
    private Long commentId;

    /** 点赞时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
