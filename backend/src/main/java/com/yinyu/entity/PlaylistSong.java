package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 歌单-歌曲关联表
 */
@Data
@TableName("playlist_song")
public class PlaylistSong {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 歌单ID -> playlist.id */
    private Long playlistId;

    /** 歌曲ID -> song.id */
    private Long songId;

    /** 歌曲在歌单中的顺序 */
    private Integer sort;

    /** 添加时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
