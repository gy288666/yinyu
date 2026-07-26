package com.yinyu.util;

import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Map 请求体的类型化取参工具（后台简单 CRUD 用，避免为每个资源建 DTO）
 */
public final class Params {

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Params() {}

    public static String str(Map<String, Object> map, String key) {
        Object v = map == null ? null : map.get(key);
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    public static String requireStr(Map<String, Object> map, String key) {
        String v = str(map, key);
        if (v == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: " + key + " 不能为空");
        }
        return v;
    }

    public static Long lng(Map<String, Object> map, String key) {
        Object v = map == null ? null : map.get(key);
        if (v == null || String.valueOf(v).isBlank()) {
            return null;
        }
        try {
            return v instanceof Number n ? n.longValue() : Long.valueOf(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: " + key + " 须为数字");
        }
    }

    public static Long requireLng(Map<String, Object> map, String key) {
        Long v = lng(map, key);
        if (v == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: " + key + " 不能为空");
        }
        return v;
    }

    public static Integer integer(Map<String, Object> map, String key) {
        Long v = lng(map, key);
        return v == null ? null : v.intValue();
    }

    public static Boolean bool(Map<String, Object> map, String key) {
        Object v = map == null ? null : map.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        String s = String.valueOf(v).trim();
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }

    public static BigDecimal dec(Map<String, Object> map, String key) {
        Object v = map == null ? null : map.get(key);
        if (v == null || String.valueOf(v).isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: " + key + " 金额格式不合法");
        }
    }

    public static LocalDateTime dateTime(Map<String, Object> map, String key) {
        String v = str(map, key);
        if (v == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(v, DATETIME);
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: " + key + " 须为 yyyy-MM-dd HH:mm:ss");
        }
    }

    public static LocalDate date(Map<String, Object> map, String key) {
        String v = str(map, key);
        if (v == null) {
            return null;
        }
        try {
            return LocalDate.parse(v);
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: " + key + " 须为 yyyy-MM-dd");
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Long> longList(Map<String, Object> map, String key) {
        Object v = map == null ? null : map.get(key);
        if (v == null) {
            return null;
        }
        if (!(v instanceof List<?> list)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: " + key + " 须为数组");
        }
        return list.stream().map(o -> {
            try {
                return o instanceof Number n ? n.longValue() : Long.valueOf(String.valueOf(o));
            } catch (NumberFormatException e) {
                throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: " + key + " 元素须为数字");
            }
        }).toList();
    }

    @SuppressWarnings("unchecked")
    public static List<String> strList(Map<String, Object> map, String key) {
        Object v = map == null ? null : map.get(key);
        if (v == null) {
            return null;
        }
        if (!(v instanceof List<?> list)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: " + key + " 须为数组");
        }
        return list.stream().map(String::valueOf).toList();
    }
}
