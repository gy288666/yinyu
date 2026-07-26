package com.yinyu.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 歌手（api.md 4.1/4.2；intro/albumCount 仅详情返回）
 */
@Data
public class SingerVO {

    private Long id;
    private String name;
    private String avatar;
    private String area;
    private Integer type;
    private Long songCount;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String intro;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long albumCount;
}
