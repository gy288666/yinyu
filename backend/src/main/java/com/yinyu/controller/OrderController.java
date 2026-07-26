package com.yinyu.controller;

import com.yinyu.common.result.PageResult;
import com.yinyu.common.result.Result;
import com.yinyu.dto.OrderCreateRequest;
import com.yinyu.security.AuthContext;
import com.yinyu.security.RequireUser;
import com.yinyu.service.OrderService;
import com.yinyu.vo.SongVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 会员与订单（api.md 15.x）
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 会员套餐列表（15.1） */
    @GetMapping("/vip/plans")
    public Result<List<Map<String, Object>>> plans() {
        return Result.success(orderService.plans());
    }

    /** 创建订单（15.2） */
    @PostMapping("/orders")
    @RequireUser
    public Result<Map<String, Object>> create(@Valid @RequestBody OrderCreateRequest req) {
        return Result.success(orderService.create(req, AuthContext.userId()));
    }

    /** 发起支付（15.3） */
    @PostMapping("/orders/{orderNo}/pay")
    @RequireUser
    public Result<Map<String, Object>> pay(@PathVariable String orderNo,
                                           @RequestBody(required = false) Map<String, String> body) {
        String channel = body == null ? "MOCK" : body.getOrDefault("channel", "MOCK");
        return Result.success(orderService.pay(orderNo, channel, AuthContext.userId()));
    }

    /** 支付宝异步回调（15.4）：纯文本响应，不走统一响应体 */
    @PostMapping("/orders/alipay/notify")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        return orderService.alipayNotify(params);
    }

    /** 我的订单（15.6） */
    @GetMapping("/orders")
    @RequireUser
    public Result<PageResult<Map<String, Object>>> myOrders(@RequestParam(required = false) String status,
                                                            @RequestParam(defaultValue = "1") long pageNum,
                                                            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(orderService.myOrders(AuthContext.userId(), status, pageNum, pageSize));
    }

    /** 订单查询（15.5） */
    @GetMapping("/orders/{orderNo}")
    @RequireUser
    public Result<Map<String, Object>> detail(@PathVariable String orderNo) {
        return Result.success(orderService.detail(orderNo, AuthContext.userId()));
    }

    /** 我的已购单曲（15.7） */
    @GetMapping("/purchases/songs")
    @RequireUser
    public Result<PageResult<SongVO>> purchases(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(orderService.purchasedSongs(AuthContext.userId(), pageNum, pageSize));
    }
}
