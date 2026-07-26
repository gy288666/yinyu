package com.yinyu.vo;

import lombok.Data;

import java.util.List;

/**
 * 歌单（api.md 6.1）
 */
@Data
public class PlaylistVO {

    private Long id;
    private String title;
    private String cover;
    /** 本期库表无歌单标签关系，恒为空数组，二期实现 */
    private List<String> tags;
    private Long playCount;
    private Integer songCount;
    private String creatorName;
    /** 是否官方运营歌单 */
    private Boolean official;
    /** 是否内置歌单（我喜欢的音乐） */
    private Boolean builtin;
    /** 收藏列表中失效项标记 */
    private Boolean invalid;
}
