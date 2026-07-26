package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 订单表（会员/单曲购买）
 */
@Data
@TableName("order_info")
public class OrderInfo {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务订单号（雪花/时间戳规则生成） */
    private String orderNo;

    /** 下单用户ID -> user.id */
    private Long userId;

    /** 订单类型：1-会员套餐 2-单曲购买 */
    private Integer orderType;

    /** 商品ID：order_type=1 指向 vip_package.id，=2 指向 song.id */
    private Long targetId;

    /** 商品名称快照（下单时冗余，防商品改名） */
    private String targetName;

    /** 应付金额快照（元） */
    private BigDecimal amount;

    /** 支付渠道：1-支付宝 2-微信 3-模拟支付 */
    private Integer payChannel;

    /** 订单状态：0-待支付 1-已支付 2-超时关闭 3-已退款 */
    private Integer status;

    /** 支付截止时间（超时任务据此关单） */
    private LocalDateTime expireTime;

    /** 支付成功时间 */
    private LocalDateTime payTime;

    /** 关闭/退款时间 */
    private LocalDateTime closeTime;

    /** 第三方支付流水号 */
    private String transactionId;

    /** 备注 */
    private String remark;

    /** 下单时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
