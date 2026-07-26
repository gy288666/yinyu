package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 歌单表
 */
@Data
@TableName("playlist")
public class Playlist {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 歌单名称 */
    private String name;

    /** 创建者用户ID -> user.id；官方歌单为 0 */
    private Long userId;

    /** 类型：0-用户自建 1-官方运营 */
    private Integer type;

    /** 歌单封面路径 */
    private String cover;

    /** 歌单简介 */
    private String introduction;

    /** 歌曲数（冗余计数） */
    private Integer songCount;

    /** 播放量（冗余计数） */
    private Long playCount;

    /** 被收藏数（冗余计数） */
    private Long collectCount;

    /** 是否公开：0-私密 1-公开 */
    private Integer isPublic;

    /** 状态：0-封禁/隐藏 1-正常 */
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
