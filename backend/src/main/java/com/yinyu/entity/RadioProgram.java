package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 电台节目表
 */
@Data
@TableName("radio_program")
public class RadioProgram {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 电台ID -> radio.id */
    private Long radioId;

    /** 节目标题 */
    private String title;

    /** 关联曲库歌曲ID -> song.id，可为空 */
    private Long songId;

    /** 独立音频对象路径，song_id 为空时使用 */
    private String filePath;

    /** 时长（秒） */
    private Integer duration;

    /** 上线时间 */
    private LocalDateTime publishTime;

    /** 节目序号 */
    private Integer sort;

    /** 状态：0-下架 1-上架 */
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
