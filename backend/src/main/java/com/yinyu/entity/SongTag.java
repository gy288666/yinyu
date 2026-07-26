package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 歌曲-标签关联表
 */
@Data
@TableName("song_tag")
public class SongTag {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 歌曲ID -> song.id */
    private Long songId;

    /** 标签ID -> tag.id */
    private Long tagId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
