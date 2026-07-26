package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 操作日志表
 */
@Data
@TableName("operation_log")
public class OperationLog {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人ID -> admin.id */
    private Long adminId;

    /** 功能模块，如 音乐管理 */
    private String module;

    /** 操作描述，如 审核通过歌曲 */
    private String operation;

    /** 请求方法/接口路径 */
    private String method;

    /** 请求参数（JSON） */
    private String params;

    /** 操作IP */
    private String ip;

    /** 结果：0-失败 1-成功 */
    private Integer status;

    /** 失败异常信息 */
    private String errorMsg;

    /** 耗时（毫秒） */
    private Long costTime;

    /** 操作时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
