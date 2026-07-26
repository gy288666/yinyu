package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 歌曲表
 */
@Data
@TableName("song")
public class Song {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 歌曲名称 */
    private String name;

    /** 所属专辑ID -> album.id，单曲可为空 */
    private Long albumId;

    /** 分类ID -> category.id */
    private Long categoryId;

    /** 歌曲封面路径（为空时取专辑封面） */
    private String cover;

    /** 音频对象路径（MinIO music 桶内相对路径） */
    private String filePath;

    /** 歌词文件（.lrc）对象路径 */
    private String lyricPath;

    /** 时长（秒） */
    private Integer duration;

    /** 音频文件大小（字节） */
    private Long fileSize;

    /** 是否原创：0-否 1-是（原创榜数据来源） */
    private Integer isOriginal;

    /** 付费类型：0-免费 1-会员免费 2-单曲购买 */
    private Integer payType;

    /** 单曲购买价格（元），pay_type=2 时有效 */
    private BigDecimal price;

    /** 累计播放量（Redis 计数定时回写） */
    private Long playCount;

    /** 累计点赞/喜欢数（冗余计数） */
    private Long likeCount;

    /** 累计被收藏进歌单数（冗余计数） */
    private Long collectCount;

    /** 累计下载数（冗余计数） */
    private Long downloadCount;

    /** 评论数（冗余计数） */
    private Long commentCount;

    /** 审核状态：0-待审核 1-审核通过 2-审核驳回 */
    private Integer auditStatus;

    /** 上架状态：0-下架 1-上架（仅审核通过后可上架） */
    private Integer status;

    /** 上传/录入管理员ID -> admin.id */
    private Long uploadAdminId;

    /** 发布（上架）时间，新歌榜数据来源 */
    private LocalDateTime publishTime;

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
