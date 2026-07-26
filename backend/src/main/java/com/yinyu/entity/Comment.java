package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 评论表
 */
@Data
@TableName("comment")
public class Comment {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 评论用户ID -> user.id */
    private Long userId;

    /** 目标类型：1-歌曲 2-歌单 3-专辑 */
    private Integer targetType;

    /** 目标ID（随 target_type 指向 song/playlist/album） */
    private Long targetId;

    /** 评论内容 */
    private String content;

    /** 根评论ID，0=一级评论 */
    private Long parentId;

    /** 被回复用户ID -> user.id */
    private Long replyUserId;

    /** 点赞数（冗余计数） */
    private Integer likeCount;

    /** 状态：0-屏蔽 1-正常 */
    private Integer status;

    /** 评论时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
