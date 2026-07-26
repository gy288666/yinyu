package com.yinyu.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;
import com.yinyu.common.result.PageResult;
import com.yinyu.common.result.Result;
import com.yinyu.entity.Admin;
import com.yinyu.entity.OperationLog;
import com.yinyu.mapper.AdminMapper;
import com.yinyu.mapper.OperationLogMapper;
import com.yinyu.security.RequireAdmin;
import com.yinyu.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统设置与操作日志（api.md 18.9）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminSystemController {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SystemConfigService systemConfigService;
    private final OperationLogMapper operationLogMapper;
    private final AdminMapper adminMapper;

    /** 设置查询（18.9.1） */
    @GetMapping("/settings")
    @RequireAdmin(permission = "system:setting:view")
    public Result<Map<String, String>> settings() {
        return Result.success(systemConfigService.settings());
    }

    /** 设置修改（18.9.2）：键值对，仅传需修改项 */
    @PutMapping("/settings")
    @RequireAdmin(permission = "system:setting:edit")
    public Result<Void> updateSettings(@RequestBody Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: 请求体不能为空");
        }
        systemConfigService.update(body);
        return Result.success();
    }

    /** 操作日志分页（18.9.3） */
    @GetMapping("/logs")
    @RequireAdmin(permission = "system:log:list")
    public Result<PageResult<Map<String, Object>>> logs(@RequestParam(defaultValue = "1") long pageNum,
                                                        @RequestParam(defaultValue = "10") long pageSize,
                                                        @RequestParam(required = false) String adminName,
                                                        @RequestParam(required = false) String module,
                                                        @RequestParam(required = false) String startTime,
                                                        @RequestParam(required = false) String endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .like(module != null && !module.isEmpty(), OperationLog::getModule, module)
                .orderByDesc(OperationLog::getCreateTime);
        if (adminName != null && !adminName.isEmpty()) {
            List<Long> adminIds = adminMapper.selectList(new LambdaQueryWrapper<Admin>()
                            .and(w -> w.like(Admin::getUsername, adminName)
                                    .or().like(Admin::getNickname, adminName)))
                    .stream().map(Admin::getId).toList();
            if (adminIds.isEmpty()) {
                return Result.success(PageResult.of(pageNum, pageSize, 0, List.of()));
            }
            wrapper.in(OperationLog::getAdminId, adminIds);
        }
        if (startTime != null && !startTime.isBlank()) {
            wrapper.ge(OperationLog::getCreateTime, parse(startTime));
        }
        if (endTime != null && !endTime.isBlank()) {
            wrapper.le(OperationLog::getCreateTime, parse(endTime));
        }
        Page<OperationLog> page = operationLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Long> adminIds = page.getRecords().stream().map(OperationLog::getAdminId).distinct().toList();
        Map<Long, String> names = adminIds.isEmpty() ? Map.of()
                : adminMapper.selectBatchIds(adminIds).stream()
                        .collect(Collectors.toMap(Admin::getId,
                                a -> a.getUsername() == null ? "" : a.getUsername()));
        return Result.success(PageResult.from(page, list -> list.stream().map(log -> {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", log.getId());
            vo.put("adminName", names.getOrDefault(log.getAdminId(), String.valueOf(log.getAdminId())));
            vo.put("module", log.getModule());
            vo.put("action", log.getOperation());
            vo.put("method", log.getMethod());
            vo.put("params", log.getParams());
            vo.put("ip", log.getIp());
            vo.put("costMs", log.getCostTime());
            vo.put("success", log.getStatus() != null && log.getStatus() == 1);
            vo.put("errorMsg", log.getErrorMsg());
            vo.put("createTime", log.getCreateTime());
            return vo;
        }).toList()));
    }

    private LocalDateTime parse(String v) {
        try {
            return LocalDateTime.parse(v.trim(), DT);
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: 时间格式须为 yyyy-MM-dd HH:mm:ss");
        }
    }
}
