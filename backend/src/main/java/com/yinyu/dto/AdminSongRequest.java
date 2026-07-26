package com.yinyu.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 后台新增/修改音乐（api.md 18.2.3/18.2.4）
 */
@Data
public class AdminSongRequest {

    @Size(max = 150, message = "歌曲名过长")
    private String name;

    private Long singerId;
    private Long albumId;
    private Long categoryId;

    /** 18.2.2 上传返回的音频对象路径 */
    private String objectKey;

    private String cover;
    /** 歌词对象路径 */
    private String lyric;
    /** 是否 VIP 曲目 */
    private Boolean vip;
    private BigDecimal price;
    /** standard / lossless（本期仅存档参考，实际按文件后缀推断） */
    private String quality;
    /** 是否原创 */
    private Boolean original;
    private Integer duration;
    private Long fileSize;
}
