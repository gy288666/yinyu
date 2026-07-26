package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 歌曲-歌手关联表（合唱多对多）
 */
@Data
@TableName("song_singer")
public class SongSinger {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 歌曲ID -> song.id */
    private Long songId;

    /** 歌手ID -> singer.id */
    private Long singerId;

    /** 署名顺序，0 为主唱/第一署名 */
    private Integer sort;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
