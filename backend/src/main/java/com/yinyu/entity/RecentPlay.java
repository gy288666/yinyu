package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 最近播放表
 */
@Data
@TableName("recent_play")
public class RecentPlay {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID -> user.id */
    private Long userId;

    /** 歌曲ID -> song.id */
    private Long songId;

    /** 最近一次播放时间 */
    private LocalDateTime playTime;

    /** 该用户对该歌曲的累计播放次数 */
    private Integer playCount;
}
