package com.yinyu.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 歌单详情（api.md 6.3）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlaylistDetailVO extends PlaylistVO {

    private String intro;
    /** PUBLIC / PRIVATE */
    private String visibility;
    /** 官方歌单为 null */
    private Long creatorId;
    private Long collectCount;
    private Boolean collected;
    private List<SongVO> songs;
}
