package com.yinyu.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;
import com.yinyu.common.result.PageResult;
import com.yinyu.common.result.Result;
import com.yinyu.entity.OrderInfo;
import com.yinyu.entity.VipPackage;
import com.yinyu.mapper.OrderInfoMapper;
import com.yinyu.mapper.VipPackageMapper;
import com.yinyu.security.RequireAdmin;
import com.yinyu.service.OrderService;
import com.yinyu.util.Params;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 后台订单与套餐管理（api.md 18.7）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminOrderController {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OrderService orderService;
    private final VipPackageMapper vipPackageMapper;
    private final OrderInfoMapper orderInfoMapper;

    // ---------------- 订单 ----------------

    @GetMapping("/orders")
    @RequireAdmin(permission = "order:list")
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                        @RequestParam(defaultValue = "10") long pageSize,
                                                        @RequestParam(required = false) String orderNo,
                                                        @RequestParam(required = false) Long userId,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) String startTime,
                                                        @RequestParam(required = false) String endTime) {
        return Result.success(orderService.adminPage(pageNum, pageSize, orderNo, userId, status,
                parse(startTime), parse(endTime)));
    }

    private LocalDateTime parse(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(v.trim(), DT);
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: 时间格式须为 yyyy-MM-dd HH:mm:ss");
        }
    }

    @GetMapping("/orders/{orderNo}")
    @RequireAdmin(permission = "order:list")
    public Result<Map<String, Object>> detail(@PathVariable String orderNo) {
        return Result.success(orderService.adminDetail(orderNo));
    }

    /** 关闭待支付订单 */
    @PutMapping("/orders/{orderNo}/close")
    @RequireAdmin(permission = "order:close")
    public Result<Void> close(@PathVariable String orderNo,
                              @RequestBody(required = false) Map<String, Object> body) {
        orderService.adminClose(orderNo, Params.str(body, "reason"));
        return Result.success();
    }

    /** 标记退款（18.7.2）：回收权益 */
    @PutMapping("/orders/{orderNo}/refund")
    @RequireAdmin(permission = "order:refund")
    public Result<Void> refund(@PathVariable String orderNo, @RequestBody Map<String, Object> body) {
        orderService.adminRefund(orderNo, Params.requireStr(body, "reason"));
        return Result.success();
    }

    // ---------------- 套餐管理（18.7.3） ----------------

    @GetMapping("/vip-plans")
    @RequireAdmin(permission = "vip:plan")
    public Result<List<Map<String, Object>>> plans() {
        return Result.success(vipPackageMapper.selectList(
                        new LambdaQueryWrapper<VipPackage>().orderByAsc(VipPackage::getSort))
                .stream().map(orderService::planVO).toList());
    }

    @PostMapping("/vip-plans")
    @RequireAdmin(permission = "vip:plan")
    public Result<Map<String, Object>> planCreate(@RequestBody Map<String, Object> body) {
        VipPackage plan = new VipPackage();
        plan.setName(Params.requireStr(body, "name"));
        Integer days = Params.integer(body, "days");
        BigDecimal price = Params.dec(body, "price");
        if (days == null || days <= 0 || price == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: days/price 不能为空");
        }
        plan.setDays(days);
        plan.setPrice(price);
        plan.setOriginalPrice(Params.dec(body, "originPrice"));
        Integer sort = Params.integer(body, "sort");
        plan.setSort(sort == null ? 0 : sort);
        Boolean enabled = Params.bool(body, "enabled");
        plan.setStatus(enabled == null || enabled ? 1 : 0);
        vipPackageMapper.insert(plan);
        return Result.success(orderService.planVO(plan));
    }

    @PutMapping("/vip-plans/{id}")
    @RequireAdmin(permission = "vip:plan")
    public Result<Void> planUpdate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        VipPackage plan = vipPackageMapper.selectById(id);
        if (plan == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        String name = Params.str(body, "name");
        if (name != null) {
            plan.setName(name);
        }
        Integer days = Params.integer(body, "days");
        if (days != null) {
            plan.setDays(days);
        }
        BigDecimal price = Params.dec(body, "price");
        if (price != null) {
            plan.setPrice(price);
        }
        if (body.containsKey("originPrice")) {
            plan.setOriginalPrice(Params.dec(body, "originPrice"));
        }
        Integer sort = Params.integer(body, "sort");
        if (sort != null) {
            plan.setSort(sort);
        }
        Boolean enabled = Params.bool(body, "enabled");
        if (enabled != null) {
            plan.setStatus(enabled ? 1 : 0);
        }
        vipPackageMapper.updateById(plan);
        return Result.success();
    }

    @DeleteMapping("/vip-plans/{id}")
    @RequireAdmin(permission = "vip:plan")
    public Result<Void> planDelete(@PathVariable Long id) {
        VipPackage plan = vipPackageMapper.selectById(id);
        if (plan == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        // 有待支付订单引用时不可删除
        if (orderInfoMapper.selectCount(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getOrderType, 1)
                .eq(OrderInfo::getTargetId, id)
                .eq(OrderInfo::getStatus, 0)) > 0) {
            throw new BizException(ErrorCode.REFERENCED_CANNOT_DELETE, "存在待支付订单引用，不可删除");
        }
        vipPackageMapper.deleteById(id);
        return Result.success();
    }
}
