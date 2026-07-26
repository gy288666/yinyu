package com.yinyu.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yinyu.entity.OperationLog;
import com.yinyu.mapper.OperationLogMapper;
import com.yinyu.security.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 操作日志切面：拦截后台控制器（controller.admin 包）的写操作（POST/PUT/DELETE），
 * 记录 模块/操作/操作人/IP/参数/耗时/结果 到 operation_log（api.md 18 通用说明）。
 * 登录/认证接口无管理员上下文时不记录。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final int PARAMS_MAX_LEN = 2000;

    /** 控制器类名 -> 模块名 */
    private static final Map<String, String> MODULES = Map.ofEntries(
            Map.entry("AdminSongController", "音乐管理"),
            Map.entry("AdminAuditController", "音乐审核"),
            Map.entry("AdminContentController", "内容管理"),
            Map.entry("AdminPlaylistController", "歌单管理"),
            Map.entry("AdminCopyrightController", "版权管理"),
            Map.entry("AdminUserController", "用户运营"),
            Map.entry("AdminOrderController", "订单管理"),
            Map.entry("AdminOpsController", "运营内容"),
            Map.entry("AdminRbacController", "权限管理"),
            Map.entry("AdminSystemController", "系统设置"),
            Map.entry("AdminStatsController", "数据统计"),
            Map.entry("AdminCommentController", "评论管理"),
            Map.entry("AdminDashboardController", "数据看板"));

    /** 方法名前缀 -> 操作名 */
    private static final Map<String, String> ACTIONS = Map.ofEntries(
            Map.entry("create", "新增"), Map.entry("add", "新增"),
            Map.entry("update", "修改"), Map.entry("edit", "修改"),
            Map.entry("delete", "删除"), Map.entry("remove", "删除"),
            Map.entry("pass", "审核通过"), Map.entry("reject", "审核驳回"),
            Map.entry("upload", "上传"), Map.entry("reset", "重置密码"),
            Map.entry("close", "关闭"), Map.entry("refund", "退款"),
            Map.entry("grant", "授权"), Map.entry("adjust", "调整"),
            Map.entry("handle", "处理"), Map.entry("replace", "维护"),
            Map.entry("change", "变更"), Map.entry("generate", "生成"));

    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    @Pointcut("within(com.yinyu.controller.admin..*) && ("
            + "@annotation(org.springframework.web.bind.annotation.PostMapping)"
            + " || @annotation(org.springframework.web.bind.annotation.PutMapping)"
            + " || @annotation(org.springframework.web.bind.annotation.DeleteMapping))")
    public void adminWriteOperation() {
    }

    @Around("adminWriteOperation()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Throwable error = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            try {
                record(joinPoint, System.currentTimeMillis() - start, error);
            } catch (Exception e) {
                log.warn("操作日志记录失败: {}", e.getMessage());
            }
        }
    }

    private void record(ProceedingJoinPoint joinPoint, long costMs, Throwable error) {
        Long adminId = AuthContext.adminId();
        if (adminId == null) {
            return; // 登录等无管理员上下文的接口不记录
        }
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        OperationLog logRow = new OperationLog();
        logRow.setAdminId(adminId);
        logRow.setModule(MODULES.getOrDefault(className, className));
        logRow.setOperation(actionName(methodName));
        logRow.setParams(serializeArgs(joinPoint.getArgs()));
        logRow.setCostTime(costMs);
        logRow.setStatus(error == null ? 1 : 0);
        if (error != null) {
            String msg = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
            logRow.setErrorMsg(msg.length() > 1000 ? msg.substring(0, 1000) : msg);
        }
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            logRow.setMethod(request.getMethod() + " " + request.getRequestURI());
            logRow.setIp(clientIp(request));
        }
        operationLogMapper.insert(logRow);
    }

    private String actionName(String methodName) {
        String lower = methodName.toLowerCase();
        for (Map.Entry<String, String> e : ACTIONS.entrySet()) {
            if (lower.contains(e.getKey())) {
                return e.getValue() + "（" + methodName + "）";
            }
        }
        return methodName;
    }

    private String serializeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        List<Object> loggable = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null || arg instanceof HttpServletRequest || arg instanceof HttpServletResponse) {
                continue;
            }
            if (arg instanceof MultipartFile file) {
                loggable.add(Map.of("file", String.valueOf(file.getOriginalFilename()),
                        "size", file.getSize()));
            } else {
                loggable.add(arg);
            }
        }
        try {
            String json = objectMapper.writeValueAsString(
                    loggable.size() == 1 ? loggable.get(0) : loggable);
            return json.length() > PARAMS_MAX_LEN ? json.substring(0, PARAMS_MAX_LEN) : json;
        } catch (Exception e) {
            return String.valueOf(loggable);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
