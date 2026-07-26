package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 歌曲审核记录表
 */
@Data
@TableName("song_audit_record")
public class SongAuditRecord {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 歌曲ID -> song.id */
    private Long songId;

    /** 审核结论：1-通过 2-驳回 */
    private Integer auditStatus;

    /** 审核意见/驳回原因 */
    private String reason;

    /** 审核人ID -> admin.id */
    private Long adminId;

    /** 审核时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
