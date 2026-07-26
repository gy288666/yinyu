package com.yinyu.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建/修改歌单（api.md 6.5/6.6）
 */
@Data
public class PlaylistRequest {

    @Size(max = 100, message = "标题过长")
    private String title;

    private String cover;

    @Size(max = 500, message = "简介过长")
    private String intro;

    /** PUBLIC / PRIVATE */
    @Pattern(regexp = "^(PUBLIC|PRIVATE)$", message = "visibility 须为 PUBLIC/PRIVATE")
    private String visibility;
}
