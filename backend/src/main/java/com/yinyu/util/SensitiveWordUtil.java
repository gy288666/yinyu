package com.yinyu.util;

import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;

import java.util.List;

/**
 * 敏感词校验（简单包含匹配，演示实现；生产可替换为 DFA/词库服务）
 */
public final class SensitiveWordUtil {

    private static final List<String> WORDS = List.of(
            "赌博", "毒品", "色情", "诈骗", "傻逼", "去死", "fuck", "shit");

    private SensitiveWordUtil() {}

    public static boolean contains(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        String lower = content.toLowerCase();
        return WORDS.stream().anyMatch(lower::contains);
    }

    /** 含敏感词抛 40001 */
    public static void check(String content) {
        if (contains(content)) {
            throw new BizException(ErrorCode.SENSITIVE_CONTENT, "内容包含敏感词");
        }
    }
}
