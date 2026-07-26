package com.yinyu.common.constant;

/**
 * Redis 键约定
 */
public final class RedisKeys {

    private RedisKeys() {}

    /** 单曲待回写播放增量：play:count:{songId}（定时任务每5分钟 GETDEL 回写） */
    public static final String PLAY_COUNT_PREFIX = "play:count:";
    /** 日播放榜 ZSET：rank:day:{yyyyMMdd}，member=songId */
    public static final String RANK_DAY_PREFIX = "rank:day:";
    /** 播放埋点去重：play:dedup:{uid|ip}:{songId}，30 秒 */
    public static final String PLAY_DEDUP_PREFIX = "play:dedup:";
    /** token 黑名单：jwt:black:{md5(token)} */
    public static final String JWT_BLACKLIST_PREFIX = "jwt:black:";
    /** 登录失败计数：login:fail:{USER|ADMIN}:{username} */
    public static final String LOGIN_FAIL_PREFIX = "login:fail:";
    /** 登录锁定：login:lock:{USER|ADMIN}:{username} */
    public static final String LOGIN_LOCK_PREFIX = "login:lock:";
    /** 管理员验证码：captcha:admin:{captchaKey} */
    public static final String ADMIN_CAPTCHA_PREFIX = "captcha:admin:";
    /** 管理员权限码缓存：admin:perms:{adminId} */
    public static final String ADMIN_PERMS_PREFIX = "admin:perms:";
    /** 管理员角色缓存：admin:roles:{adminId} */
    public static final String ADMIN_ROLES_PREFIX = "admin:roles:";
    /** 热搜词 ZSET */
    public static final String SEARCH_HOT = "search:hot";
    /** 私人FM 不喜欢 ZSET：fm:dislike:{userId}，score=过期时间戳（7 天） */
    public static final String FM_DISLIKE_PREFIX = "fm:dislike:";
    /** 电台一轮已播集合：radio:played:{radioId}:{uid|guest} */
    public static final String RADIO_PLAYED_PREFIX = "radio:played:";
    /** 每日推荐缓存：rec:daily:{yyyyMMdd}:{uid|guest}（当日有效） */
    public static final String REC_DAILY_PREFIX = "rec:daily:";
    /** 门户用户停用标记：user:disabled:{userId}（后台停用即时生效） */
    public static final String USER_DISABLED_PREFIX = "user:disabled:";
}
