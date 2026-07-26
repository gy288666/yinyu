package com.yinyu.common.exception;

import lombok.Getter;

/**
 * 业务异常：携带 api.md 错误码
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
