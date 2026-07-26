package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户收藏歌单表
 */
@Data
@TableName("user_collect_playlist")
public class UserCollectPlaylist {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID -> user.id */
    private Long userId;

    /** 歌单ID -> playlist.id */
    private Long playlistId;

    /** 收藏时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
