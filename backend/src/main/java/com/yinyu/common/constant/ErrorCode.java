package com.yinyu.common.constant;

/**
 * 错误码常量（对齐 docs/api.md 1.5 错误码表）
 */
public final class ErrorCode {

    private ErrorCode() {}

    public static final int SUCCESS = 0;

    public static final int PARAM_INVALID = 10001;          // 参数校验失败
    public static final int USERNAME_EXISTS = 10002;        // 用户名已存在
    public static final int BAD_CREDENTIALS = 10003;        // 用户名或密码错误
    public static final int ACCOUNT_LOCKED = 10004;         // 账号已锁定（连续错5次锁10分钟）
    public static final int ACCOUNT_DISABLED = 10005;       // 账号已停用
    public static final int CAPTCHA_ERROR = 10006;          // 验证码错误或过期
    public static final int TOKEN_INVALID = 10007;          // token 无效或已过期（HTTP 401）
    public static final int REFRESH_TOKEN_INVALID = 10008;  // refreshToken 已使用或无效

    public static final int FILE_INVALID = 20001;           // 文件类型或大小不合法
    public static final int RESOURCE_NOT_FOUND = 20002;     // 资源不存在
    public static final int SONG_UNAVAILABLE = 20003;       // 歌曲已下架或不可用
    public static final int STATE_NOT_ALLOWED = 20004;      // 资源状态不允许该操作
    public static final int REFERENCED_CANNOT_DELETE = 20005; // 存在引用不可删除
    public static final int PRIVATE_RESOURCE = 20006;       // 无权访问该私密资源

    public static final int VIP_REQUIRED = 30001;           // VIP 专享
    public static final int PURCHASE_REQUIRED = 30002;      // 付费单曲需购买
    public static final int DOWNLOAD_QUOTA_EXCEEDED = 30003;// 今日下载配额已用完
    public static final int ORDER_NOT_FOUND = 30004;        // 订单不存在或已关闭
    public static final int DUPLICATE_PAY = 30005;          // 重复支付/回调已处理

    public static final int SENSITIVE_CONTENT = 40001;      // 内容包含敏感词

    public static final int FORBIDDEN = 40301;              // 无操作权限（HTTP 403）
    public static final int SYSTEM_ERROR = 50000;           // 系统繁忙（HTTP 500）
}
