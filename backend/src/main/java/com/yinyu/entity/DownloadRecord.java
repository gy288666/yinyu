package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 下载记录表
 */
@Data
@TableName("download_record")
public class DownloadRecord {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID -> user.id */
    private Long userId;

    /** 歌曲ID -> song.id */
    private Long songId;

    /** 下载时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
