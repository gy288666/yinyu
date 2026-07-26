package com.yinyu.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台音乐列表项（api.md 18.2.1）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminSongVO extends SongVO {

    /** PENDING/ONLINE/REJECTED/OFFLINE */
    private String status;
    private String rejectReason;
    private String objectKey;
    private Long fileSize;
    /** 码率 kbps（无源数据时按大小/时长估算） */
    private Integer bitrate;
    private String createBy;
    private Boolean original;
}
