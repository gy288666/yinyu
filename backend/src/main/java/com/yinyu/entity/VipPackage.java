package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 会员套餐表
 */
@Data
@TableName("vip_package")
public class VipPackage {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 套餐名称，如 月卡/季卡/年卡 */
    private String name;

    /** 会员时长（天） */
    private Integer days;

    /** 售价（元） */
    private BigDecimal price;

    /** 划线原价（元） */
    private BigDecimal originalPrice;

    /** 排序值 */
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
