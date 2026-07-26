package com.yinyu.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;

/**
 * 专辑（api.md 5.1/5.2；intro/collected 仅详情返回）
 */
@Data
public class AlbumVO {

    private Long id;
    private String name;
    private String cover;
    private Long singerId;
    private String singerName;
    private LocalDate publishDate;
    private Integer songCount;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String intro;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean collected;
}
