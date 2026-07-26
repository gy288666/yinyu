package com.yinyu.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 门户 Song 结构（api.md 3.1）
 */
@Data
public class SongVO {

    private Long id;
    private String name;
    private Integer duration;
    private Long singerId;
    private String singerName;
    private Long albumId;
    private String albumName;
    private String cover;
    private Long categoryId;
    private String categoryName;
    /** 是否 VIP 曲目（pay_type=1） */
    private Boolean vip;
    /** 单曲价格，两位小数字符串 */
    private String price;
    /** standard / lossless（按音频后缀推断） */
    private String quality;
    private Long playCount;
    private LocalDateTime publishTime;
}
