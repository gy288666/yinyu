package com.yinyu.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 歌曲详情（api.md 3.2）：Song + lyric + liked + purchased
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SongDetailVO extends SongVO {

    /** 歌词（本期返回歌词文件访问地址；纯音乐可为空） */
    private String lyric;
    /** 登录时是否已喜欢 */
    private Boolean liked;
    /** 是否已购买 */
    private Boolean purchased;
}
