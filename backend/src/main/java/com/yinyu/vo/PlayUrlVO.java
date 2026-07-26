package com.yinyu.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 播放地址响应（api.md 3.3）
 */
@Data
@AllArgsConstructor
public class PlayUrlVO {

    private Long songId;
    private String url;
    private Integer expiresIn;
    /** 游客播放普通曲目试听 60 秒，其余 0 */
    private Integer trialSeconds;
}
