package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 歌手表
 */
@Data
@TableName("singer")
public class Singer {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 歌手/乐队名称 */
    private String name;

    /** 名称拼音，用于检索与首字母索引 */
    private String pinyin;

    /** 类型：1-男歌手 2-女歌手 3-乐队/组合 */
    private Integer type;

    /** 地区：内地/港台/欧美/日韩等 */
    private String region;

    /** 头像路径 */
    private String avatar;

    /** 详情页背景图路径 */
    private String cover;

    /** 歌手简介 */
    private String introduction;

    /** 热度值（冗余，定时任务更新） */
    private Long hot;

    /** 状态：0-隐藏 1-展示 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
