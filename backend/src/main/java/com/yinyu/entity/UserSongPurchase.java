package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户已购单曲表
 */
@Data
@TableName("user_song_purchase")
public class UserSongPurchase {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID -> user.id */
    private Long userId;

    /** 歌曲ID -> song.id */
    private Long songId;

    /** 来源订单ID -> order_info.id */
    private Long orderId;

    /** 购买时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
