package com.yinyu.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户表
 */
@Data
@TableName("user")
public class User {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    private String username;

    /** 密码（BCrypt 加密） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像路径 */
    private String avatar;

    /** 性别：0-未知 1-男 2-女 */
    private Integer gender;

    /** 生日 */
    private LocalDate birthday;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 个性签名 */
    private String signature;

    /** 当前等级ID -> user_level.id */
    private Long levelId;

    /** 成长值（听歌/签到等行为累计） */
    private Long exp;

    /** 是否有效会员：0-否 1-是（冗余，以 vip_expire_time 为准） */
    private Integer isVip;

    /** 会员到期时间 */
    private LocalDateTime vipExpireTime;

    /** 账号状态：0-封禁 1-正常 */
    private Integer status;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 注册时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;

    /** æ³¨å†Œæ¸ é“ direct/search/share/activity */
    private String registerChannel;
}
