package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户反馈表
 */
@Data
@TableName("feedback")
public class Feedback {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 反馈用户ID -> user.id */
    private Long userId;

    /** 反馈类型：1-功能异常 2-产品建议 3-版权投诉 4-其他 */
    private Integer type;

    /** 反馈内容 */
    private String content;

    /** 截图路径，多个用英文逗号分隔 */
    private String images;

    /** 联系方式 */
    private String contact;

    /** 处理状态：0-待处理 1-已处理 */
    private Integer status;

    /** 处理回复 */
    private String reply;

    /** 处理管理员ID -> admin.id */
    private Long handleAdminId;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 提交时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
