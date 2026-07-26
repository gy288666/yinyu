package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;
import com.yinyu.common.result.PageResult;
import com.yinyu.dto.OrderCreateRequest;
import com.yinyu.entity.OrderInfo;
import com.yinyu.entity.Song;
import com.yinyu.entity.User;
import com.yinyu.entity.UserSongPurchase;
import com.yinyu.entity.VipPackage;
import com.yinyu.mapper.OrderInfoMapper;
import com.yinyu.mapper.SongMapper;
import com.yinyu.mapper.UserMapper;
import com.yinyu.mapper.UserSongPurchaseMapper;
import com.yinyu.mapper.VipPackageMapper;
import com.yinyu.vo.SongVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 会员套餐与订单：套餐列表/下单/模拟支付/回调/查询/已购单曲 + 后台订单管理（api.md 15.x / 18.7）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VipPackageMapper vipPackageMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final UserMapper userMapper;
    private final SongMapper songMapper;
    private final UserSongPurchaseMapper userSongPurchaseMapper;
    private final SystemConfigService systemConfigService;
    private final SongAssembler songAssembler;

    // ---------------- 套餐 ----------------

    /** 会员套餐列表（api.md 15.1） */
    public List<Map<String, Object>> plans() {
        return vipPackageMapper.selectList(new LambdaQueryWrapper<VipPackage>()
                        .eq(VipPackage::getStatus, 1).orderByAsc(VipPackage::getSort))
                .stream().map(this::planVO).toList();
    }

    public Map<String, Object> planVO(VipPackage p) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", p.getId());
        vo.put("name", p.getName());
        vo.put("days", p.getDays());
        vo.put("price", money(p.getPrice()));
        vo.put("originPrice", p.getOriginalPrice() == null ? null : money(p.getOriginalPrice()));
        vo.put("sort", p.getSort());
        vo.put("status", p.getStatus());
        return vo;
    }

    private static String money(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    // ---------------- 下单 / 支付 ----------------

    /** 创建订单（api.md 15.2）：同商品待支付订单直接返回；已购单曲重复下单 20004 */
    @Transactional
    public Map<String, Object> create(OrderCreateRequest req, Long userId) {
        int orderType;
        long targetId;
        String targetName;
        BigDecimal amount;
        if ("VIP".equals(req.getOrderType())) {
            if (req.getPlanId() == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: planId 不能为空");
            }
            VipPackage plan = vipPackageMapper.selectById(req.getPlanId());
            if (plan == null || plan.getStatus() == null || plan.getStatus() != 1) {
                throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "套餐不存在或已下架");
            }
            orderType = 1;
            targetId = plan.getId();
            targetName = "音域VIP·" + plan.getName();
            amount = plan.getPrice();
        } else if ("SONG".equals(req.getOrderType())) {
            if (req.getSongId() == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: songId 不能为空");
            }
            Song song = songMapper.selectById(req.getSongId());
            if (song == null || song.getAuditStatus() == null || song.getAuditStatus() != 1) {
                throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "歌曲不存在");
            }
            if (song.getPayType() == null || song.getPayType() != 2) {
                throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "该歌曲无需购买");
            }
            if (userSongPurchaseMapper.selectCount(new LambdaQueryWrapper<UserSongPurchase>()
                    .eq(UserSongPurchase::getUserId, userId)
                    .eq(UserSongPurchase::getSongId, song.getId())) > 0) {
                throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "已购买过该单曲，无需重复下单");
            }
            orderType = 2;
            targetId = song.getId();
            targetName = song.getName();
            amount = song.getPrice() == null ? BigDecimal.ZERO : song.getPrice();
        } else {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: orderType 须为 VIP/SONG");
        }

        // 同商品存在待支付订单时直接返回原订单
        OrderInfo pending = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getUserId, userId)
                .eq(OrderInfo::getOrderType, orderType)
                .eq(OrderInfo::getTargetId, targetId)
                .eq(OrderInfo::getStatus, 0)
                .gt(OrderInfo::getExpireTime, LocalDateTime.now())
                .orderByDesc(OrderInfo::getCreateTime)
                .last("LIMIT 1"));
        if (pending != null) {
            return toVO(pending);
        }

        int expireMinutes = systemConfigService.getInt("order_expire_minutes", 15);
        OrderInfo order = new OrderInfo();
        order.setOrderNo(LocalDateTime.now().format(ORDER_NO_FMT)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000)));
        order.setUserId(userId);
        order.setOrderType(orderType);
        order.setTargetId(targetId);
        order.setTargetName(targetName);
        order.setAmount(amount);
        order.setStatus(0);
        order.setExpireTime(LocalDateTime.now().plusMinutes(expireMinutes));
        orderInfoMapper.insert(order);
        return toVO(order);
    }

    /** 发起支付（api.md 15.3）：MOCK 渠道直接支付成功；ALIPAY 未配置沙箱时提示 */
    @Transactional
    public Map<String, Object> pay(String orderNo, String channel, Long userId) {
        OrderInfo order = requireOwnOrder(orderNo, userId);
        if (order.getStatus() != null && order.getStatus() == 1) {
            throw new BizException(ErrorCode.DUPLICATE_PAY, "订单已支付，请勿重复操作");
        }
        if (order.getStatus() == null || order.getStatus() != 0
                || order.getExpireTime() == null || order.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND, "订单不存在或已关闭");
        }
        if ("MOCK".equalsIgnoreCase(channel)) {
            if (!systemConfigService.getBool("pay.mock", true)) {
                throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "模拟支付未开启");
            }
            completePay(order, 3, "MOCK-" + order.getOrderNo());
            return Map.of("status", "PAID");
        }
        if ("ALIPAY".equalsIgnoreCase(channel)) {
            // 本环境未配置支付宝沙箱密钥，返回收银台占位地址；正式接入时替换为 SDK 生成的表单
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("payUrl", "https://openapi-sandbox.dl.alipaydev.com/gateway.do?out_trade_no=" + orderNo);
            data.put("hint", "支付宝沙箱未配置密钥，本环境请使用 MOCK 渠道");
            return data;
        }
        throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: channel 须为 ALIPAY/MOCK");
    }

    /** 支付宝异步回调（api.md 15.4）：幂等；本环境未配置证书，跳过验签仅按订单号处理 */
    @Transactional
    public String alipayNotify(Map<String, String> params) {
        String orderNo = params.get("out_trade_no");
        String tradeStatus = params.get("trade_status");
        if (orderNo == null) {
            return "failure";
        }
        OrderInfo order = orderInfoMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
        if (order == null) {
            return "failure";
        }
        if (order.getStatus() != null && order.getStatus() == 1) {
            return "success"; // 重复 notify 幂等
        }
        if (order.getStatus() == null || order.getStatus() != 0) {
            return "failure";
        }
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            return "failure";
        }
        completePay(order, 1, params.get("trade_no"));
        return "success";
    }

    /** 支付完成：更新订单 + 发放权益（VIP 累加时长 / 写入已购单曲） */
    private void completePay(OrderInfo order, int payChannel, String transactionId) {
        order.setStatus(1);
        order.setPayChannel(payChannel);
        order.setPayTime(LocalDateTime.now());
        order.setTransactionId(transactionId);
        orderInfoMapper.updateById(order);
        grantBenefit(order);
    }

    private void grantBenefit(OrderInfo order) {
        if (order.getOrderType() != null && order.getOrderType() == 1) {
            VipPackage plan = vipPackageMapper.selectById(order.getTargetId());
            int days = plan == null || plan.getDays() == null ? 31 : plan.getDays();
            User user = userMapper.selectById(order.getUserId());
            if (user != null) {
                LocalDateTime base = user.getVipExpireTime() != null
                        && user.getVipExpireTime().isAfter(LocalDateTime.now())
                        ? user.getVipExpireTime() : LocalDateTime.now();
                user.setVipExpireTime(base.plusDays(days));
                user.setIsVip(1);
                userMapper.updateById(user);
            }
        } else {
            Long exists = userSongPurchaseMapper.selectCount(new LambdaQueryWrapper<UserSongPurchase>()
                    .eq(UserSongPurchase::getUserId, order.getUserId())
                    .eq(UserSongPurchase::getSongId, order.getTargetId()));
            if (exists == 0) {
                UserSongPurchase purchase = new UserSongPurchase();
                purchase.setUserId(order.getUserId());
                purchase.setSongId(order.getTargetId());
                purchase.setOrderId(order.getId());
                userSongPurchaseMapper.insert(purchase);
            }
        }
    }

    /** 回收权益（后台退款）：VIP 扣减时长 / 删除已购单曲授权 */
    private void revokeBenefit(OrderInfo order) {
        if (order.getOrderType() != null && order.getOrderType() == 1) {
            VipPackage plan = vipPackageMapper.selectById(order.getTargetId());
            int days = plan == null || plan.getDays() == null ? 31 : plan.getDays();
            User user = userMapper.selectById(order.getUserId());
            if (user != null && user.getVipExpireTime() != null) {
                LocalDateTime newExpire = user.getVipExpireTime().minusDays(days);
                user.setVipExpireTime(newExpire);
                if (newExpire.isBefore(LocalDateTime.now())) {
                    user.setIsVip(0);
                }
                userMapper.updateById(user);
            }
        } else {
            userSongPurchaseMapper.delete(new LambdaQueryWrapper<UserSongPurchase>()
                    .eq(UserSongPurchase::getUserId, order.getUserId())
                    .eq(UserSongPurchase::getSongId, order.getTargetId()));
        }
    }

    // ---------------- 查询 ----------------

    /** 订单查询（api.md 15.5，仅本人） */
    public Map<String, Object> detail(String orderNo, Long userId) {
        return toVO(requireOwnOrder(orderNo, userId));
    }

    /** 我的订单（api.md 15.6） */
    public PageResult<Map<String, Object>> myOrders(Long userId, String status, long pageNum, long pageSize) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getUserId, userId)
                .orderByDesc(OrderInfo::getCreateTime);
        applyStatus(wrapper, status);
        Page<OrderInfo> page = orderInfoMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.from(page, list -> list.stream().map(this::toVO).toList());
    }

    /** 我的已购单曲（api.md 15.7） */
    public PageResult<SongVO> purchasedSongs(Long userId, long pageNum, long pageSize) {
        Page<UserSongPurchase> page = userSongPurchaseMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<UserSongPurchase>()
                        .eq(UserSongPurchase::getUserId, userId)
                        .orderByDesc(UserSongPurchase::getCreateTime));
        List<Long> songIds = page.getRecords().stream().map(UserSongPurchase::getSongId).toList();
        Map<Long, SongVO> voMap = songIds.isEmpty() ? Map.of()
                : songAssembler.toVOs(songMapper.selectBatchIds(songIds)).stream()
                        .collect(Collectors.toMap(SongVO::getId, Function.identity()));
        return PageResult.from(page, list -> list.stream()
                .map(p -> voMap.get(p.getSongId()))
                .filter(java.util.Objects::nonNull).toList());
    }

    private OrderInfo requireOwnOrder(String orderNo, Long userId) {
        OrderInfo order = orderInfoMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
        if (order == null || (userId != null && !order.getUserId().equals(userId))) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND, "订单不存在或已关闭");
        }
        return order;
    }

    public Map<String, Object> toVO(OrderInfo order) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("orderNo", order.getOrderNo());
        vo.put("userId", order.getUserId());
        vo.put("orderType", order.getOrderType() != null && order.getOrderType() == 1 ? "VIP" : "SONG");
        vo.put("subject", order.getTargetName());
        vo.put("targetId", order.getTargetId());
        vo.put("amount", money(order.getAmount()));
        int status = order.getStatus() == null ? 0 : order.getStatus();
        vo.put("status", switch (status) {
            case 1 -> "PAID";
            case 2 -> "CLOSED";
            case 3 -> "REFUNDED";
            default -> "PENDING";
        });
        vo.put("payChannel", order.getPayChannel() == null ? null : switch (order.getPayChannel()) {
            case 1 -> "ALIPAY";
            case 2 -> "WECHAT";
            default -> "MOCK";
        });
        vo.put("payTime", order.getPayTime());
        vo.put("expireAt", order.getExpireTime());
        vo.put("createTime", order.getCreateTime());
        vo.put("remark", order.getRemark());
        return vo;
    }

    private void applyStatus(LambdaQueryWrapper<OrderInfo> wrapper, String status) {
        if (status == null || status.isEmpty()) {
            return;
        }
        switch (status) {
            case "PENDING" -> wrapper.eq(OrderInfo::getStatus, 0);
            case "PAID" -> wrapper.eq(OrderInfo::getStatus, 1);
            case "CLOSED" -> wrapper.eq(OrderInfo::getStatus, 2);
            case "REFUNDED" -> wrapper.eq(OrderInfo::getStatus, 3);
            default -> throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: status 不合法");
        }
    }

    // ---------------- 后台订单管理（api.md 18.7） ----------------

    /** 订单分页（18.7.1） */
    public PageResult<Map<String, Object>> adminPage(long pageNum, long pageSize, String orderNo,
                                                     Long userId, String status,
                                                     LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<OrderInfo>()
                .like(orderNo != null && !orderNo.isEmpty(), OrderInfo::getOrderNo, orderNo)
                .eq(userId != null, OrderInfo::getUserId, userId)
                .ge(startTime != null, OrderInfo::getCreateTime, startTime)
                .le(endTime != null, OrderInfo::getCreateTime, endTime)
                .orderByDesc(OrderInfo::getCreateTime);
        applyStatus(wrapper, status);
        Page<OrderInfo> page = orderInfoMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        // 补充下单用户名
        List<Long> userIds = page.getRecords().stream().map(OrderInfo::getUserId).distinct().toList();
        Map<Long, String> usernames = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u.getUsername() == null ? "" : u.getUsername()));
        return PageResult.from(page, list -> {
            List<Map<String, Object>> vos = new ArrayList<>();
            for (OrderInfo order : list) {
                Map<String, Object> vo = toVO(order);
                vo.put("username", usernames.get(order.getUserId()));
                vos.add(vo);
            }
            return vos;
        });
    }

    /** 订单详情（后台，不限本人） */
    public Map<String, Object> adminDetail(String orderNo) {
        OrderInfo order = orderInfoMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND, "订单不存在或已关闭");
        }
        Map<String, Object> vo = toVO(order);
        User user = userMapper.selectById(order.getUserId());
        vo.put("username", user == null ? null : user.getUsername());
        vo.put("transactionId", order.getTransactionId());
        vo.put("closeTime", order.getCloseTime());
        return vo;
    }

    /** 关闭待支付订单（后台） */
    @Transactional
    public void adminClose(String orderNo, String reason) {
        OrderInfo order = orderInfoMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND, "订单不存在或已关闭");
        }
        if (order.getStatus() == null || order.getStatus() != 0) {
            throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "仅待支付订单可关闭");
        }
        order.setStatus(2);
        order.setCloseTime(LocalDateTime.now());
        order.setRemark(reason == null ? "后台手动关闭" : reason);
        orderInfoMapper.updateById(order);
    }

    /** 标记退款（18.7.2）：回收权益 */
    @Transactional
    public void adminRefund(String orderNo, String reason) {
        OrderInfo order = orderInfoMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND, "订单不存在或已关闭");
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "仅已支付订单可退款");
        }
        order.setStatus(3);
        order.setCloseTime(LocalDateTime.now());
        order.setRemark(reason);
        orderInfoMapper.updateById(order);
        revokeBenefit(order);
    }

    /** 超时关单：定时任务调用 */
    public int closeExpired() {
        List<OrderInfo> expired = orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getStatus, 0)
                .lt(OrderInfo::getExpireTime, LocalDateTime.now())
                .last("LIMIT 200"));
        for (OrderInfo order : expired) {
            order.setStatus(2);
            order.setCloseTime(LocalDateTime.now());
            order.setRemark("超时未支付自动关闭");
            orderInfoMapper.updateById(order);
        }
        return expired.size();
    }
}
