package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 轮播图表
 */
@Data
@TableName("banner")
public class Banner {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 图片路径 */
    private String image;

    /** 跳转类型：0-无 1-歌曲 2-歌单 3-专辑 4-活动 5-外链 */
    private Integer linkType;

    /** 跳转目标：业务ID或URL */
    private String linkValue;

    /** 排序值，越小越靠前 */
    private Integer sort;

    /** 生效开始时间，空=立即 */
    private LocalDateTime startTime;

    /** 生效结束时间，空=长期 */
    private LocalDateTime endTime;

    /** 状态：0-下线 1-上线 */
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
