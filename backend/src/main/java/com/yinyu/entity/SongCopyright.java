package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 歌曲版权表（二期新增，见 sql/init.sql 末尾）
 */
@Data
@TableName("song_copyright")
public class SongCopyright {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 歌曲ID -> song.id */
    private Long songId;

    /** 版权方 */
    private String owner;

    /** 授权类型：BUYOUT-买断 / LICENSE-授权期 / ORIGINAL-原创自有 */
    private String licenseType;

    /** 授权开始日期 */
    private LocalDate startDate;

    /** 授权结束日期 */
    private LocalDate endDate;

    /** 授权文件路径/URL */
    private String fileUrl;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
