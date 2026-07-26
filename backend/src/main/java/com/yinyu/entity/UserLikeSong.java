package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户喜欢歌曲表
 */
@Data
@TableName("user_like_song")
public class UserLikeSong {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID -> user.id */
    private Long userId;

    /** 歌曲ID -> song.id */
    private Long songId;

    /** 喜欢时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
