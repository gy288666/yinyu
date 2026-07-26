package com.yinyu.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 后台专辑新增/修改（api.md 18.4.7/18.4.8）
 */
@Data
public class AlbumRequest {

    @Size(max = 100, message = "专辑名过长")
    private String name;

    private String cover;
    private Long singerId;
    private LocalDate publishDate;
    private String intro;
    private String company;
}
