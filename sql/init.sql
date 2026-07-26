-- ============================================================================
-- 音域（yinyu）音乐平台 数据库初始化脚本
-- 适用环境：MySQL 8.x  存储引擎：InnoDB  字符集：utf8mb4
--
-- 说明：
-- 1. 遵循互联网项目惯例，表之间不建立物理外键，全部以普通索引表达关联关系，
--    引用完整性由应用层（Service 层）保证，详见 docs/database-design.md。
-- 2. 所有业务实体表包含 create_time / update_time / deleted（逻辑删除）字段，
--    与 MyBatis-Plus 自动填充、@TableLogic 约定保持一致；
--    纯关系表（如 song_singer）与日志/统计类表不设 deleted，采用物理删除。
-- 3. 音频、图片等文件仅在库中保存对象路径（MinIO 桶内相对路径或本地静态路径）。
-- ============================================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS yinyu DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE yinyu;

-- ============================================================================
-- 模块一：内容模块（歌手/专辑/歌曲/分类标签/歌单/电台/审核）
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 音乐分类表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`        VARCHAR(50)  NOT NULL                COMMENT '分类名称',
  `parent_id`   BIGINT       NOT NULL DEFAULT 0      COMMENT '父分类ID，0=顶级分类',
  `icon`        VARCHAR(255)          DEFAULT NULL   COMMENT '分类图标路径',
  `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序值，越小越靠前',
  `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '音乐分类表';

-- ----------------------------------------------------------------------------
-- 标签表（比分类更细粒度的多维标记：语种/场景/情绪等）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`        VARCHAR(50) NOT NULL                COMMENT '标签名称',
  `type`        TINYINT     NOT NULL DEFAULT 1      COMMENT '标签类型：1-风格 2-语种 3-场景 4-情绪',
  `sort`        INT         NOT NULL DEFAULT 0      COMMENT '排序值',
  `status`      TINYINT     NOT NULL DEFAULT 1      COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name_type` (`name`, `type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '标签表';

-- ----------------------------------------------------------------------------
-- 歌手表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `singer`;
CREATE TABLE `singer` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`         VARCHAR(100) NOT NULL                COMMENT '歌手/乐队名称',
  `pinyin`       VARCHAR(200)          DEFAULT NULL   COMMENT '名称拼音，用于检索与首字母索引',
  `type`         TINYINT      NOT NULL DEFAULT 1      COMMENT '类型：1-男歌手 2-女歌手 3-乐队/组合',
  `region`       VARCHAR(50)           DEFAULT NULL   COMMENT '地区：内地/港台/欧美/日韩等',
  `avatar`       VARCHAR(255)          DEFAULT NULL   COMMENT '头像路径',
  `cover`        VARCHAR(255)          DEFAULT NULL   COMMENT '详情页背景图路径',
  `introduction` TEXT                                 COMMENT '歌手简介',
  `hot`          BIGINT       NOT NULL DEFAULT 0      COMMENT '热度值（冗余，定时任务更新）',
  `status`       TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：0-隐藏 1-展示',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`),
  KEY `idx_type_region` (`type`, `region`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '歌手表';

-- ----------------------------------------------------------------------------
-- 专辑表（一张专辑归属一个主歌手；歌曲通过 album_id 关联专辑）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `album`;
CREATE TABLE `album` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`         VARCHAR(100) NOT NULL                COMMENT '专辑名称',
  `singer_id`    BIGINT       NOT NULL                COMMENT '主歌手ID -> singer.id',
  `cover`        VARCHAR(255)          DEFAULT NULL   COMMENT '专辑封面路径',
  `publish_date` DATE                  DEFAULT NULL   COMMENT '发行日期',
  `company`      VARCHAR(100)          DEFAULT NULL   COMMENT '发行公司',
  `introduction` TEXT                                 COMMENT '专辑简介',
  `song_count`   INT          NOT NULL DEFAULT 0      COMMENT '收录歌曲数（冗余计数）',
  `status`       TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：0-下架 1-上架',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_singer_id` (`singer_id`),
  KEY `idx_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '专辑表';

-- ----------------------------------------------------------------------------
-- 歌曲表（核心表；歌手关系见 song_singer；审核历史见 song_audit_record）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `song`;
CREATE TABLE `song` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`           VARCHAR(150)  NOT NULL                COMMENT '歌曲名称',
  `album_id`       BIGINT                 DEFAULT NULL   COMMENT '所属专辑ID -> album.id，单曲可为空',
  `category_id`    BIGINT                 DEFAULT NULL   COMMENT '分类ID -> category.id',
  `cover`          VARCHAR(255)           DEFAULT NULL   COMMENT '歌曲封面路径（为空时取专辑封面）',
  `file_path`      VARCHAR(255)  NOT NULL                COMMENT '音频对象路径（MinIO music 桶内相对路径）',
  `lyric_path`     VARCHAR(255)           DEFAULT NULL   COMMENT '歌词文件（.lrc）对象路径',
  `duration`       INT           NOT NULL DEFAULT 0      COMMENT '时长（秒）',
  `file_size`      BIGINT        NOT NULL DEFAULT 0      COMMENT '音频文件大小（字节）',
  `is_original`    TINYINT       NOT NULL DEFAULT 0      COMMENT '是否原创：0-否 1-是（原创榜数据来源）',
  `pay_type`       TINYINT       NOT NULL DEFAULT 0      COMMENT '付费类型：0-免费 1-会员免费 2-单曲购买',
  `price`          DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '单曲购买价格（元），pay_type=2 时有效',
  `play_count`     BIGINT        NOT NULL DEFAULT 0      COMMENT '累计播放量（Redis 计数定时回写）',
  `like_count`     BIGINT        NOT NULL DEFAULT 0      COMMENT '累计点赞/喜欢数（冗余计数）',
  `collect_count`  BIGINT        NOT NULL DEFAULT 0      COMMENT '累计被收藏进歌单数（冗余计数）',
  `download_count` BIGINT        NOT NULL DEFAULT 0      COMMENT '累计下载数（冗余计数）',
  `comment_count`  BIGINT        NOT NULL DEFAULT 0      COMMENT '评论数（冗余计数）',
  `audit_status`   TINYINT       NOT NULL DEFAULT 0      COMMENT '审核状态：0-待审核 1-审核通过 2-审核驳回',
  `status`         TINYINT       NOT NULL DEFAULT 1      COMMENT '上架状态：0-下架 1-上架（仅审核通过后可上架）',
  `upload_admin_id` BIGINT                DEFAULT NULL   COMMENT '上传/录入管理员ID -> admin.id',
  `publish_time`   DATETIME               DEFAULT NULL   COMMENT '发布（上架）时间，新歌榜数据来源',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`),
  KEY `idx_album_id` (`album_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_audit_status` (`audit_status`),
  KEY `idx_status_publish` (`status`, `publish_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '歌曲表';

-- ----------------------------------------------------------------------------
-- 歌曲-歌手关联表（多对多，支持合唱；纯关系表不设 deleted）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `song_singer`;
CREATE TABLE `song_singer` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `song_id`     BIGINT   NOT NULL                COMMENT '歌曲ID -> song.id',
  `singer_id`   BIGINT   NOT NULL                COMMENT '歌手ID -> singer.id',
  `sort`        INT      NOT NULL DEFAULT 0      COMMENT '署名顺序，0 为主唱/第一署名',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_song_singer` (`song_id`, `singer_id`),
  KEY `idx_singer_id` (`singer_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '歌曲-歌手关联表（合唱多对多）';

-- ----------------------------------------------------------------------------
-- 歌曲-标签关联表（多对多）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `song_tag`;
CREATE TABLE `song_tag` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `song_id`     BIGINT   NOT NULL                COMMENT '歌曲ID -> song.id',
  `tag_id`      BIGINT   NOT NULL                COMMENT '标签ID -> tag.id',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_song_tag` (`song_id`, `tag_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '歌曲-标签关联表';

-- ----------------------------------------------------------------------------
-- 歌曲审核记录表（每次审核动作追加一条，song.audit_status 保存最新状态）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `song_audit_record`;
CREATE TABLE `song_audit_record` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `song_id`      BIGINT       NOT NULL                COMMENT '歌曲ID -> song.id',
  `audit_status` TINYINT      NOT NULL                COMMENT '审核结论：1-通过 2-驳回',
  `reason`       VARCHAR(500)          DEFAULT NULL   COMMENT '审核意见/驳回原因',
  `admin_id`     BIGINT       NOT NULL                COMMENT '审核人ID -> admin.id',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
  PRIMARY KEY (`id`),
  KEY `idx_song_id` (`song_id`),
  KEY `idx_admin_id` (`admin_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '歌曲审核记录表';

-- ----------------------------------------------------------------------------
-- 歌单表（type 区分官方歌单与用户自建歌单）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `playlist`;
CREATE TABLE `playlist` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`          VARCHAR(100) NOT NULL                COMMENT '歌单名称',
  `user_id`       BIGINT       NOT NULL DEFAULT 0      COMMENT '创建者用户ID -> user.id；官方歌单为 0',
  `type`          TINYINT      NOT NULL DEFAULT 0      COMMENT '类型：0-用户自建 1-官方运营',
  `cover`         VARCHAR(255)          DEFAULT NULL   COMMENT '歌单封面路径',
  `introduction`  VARCHAR(500)          DEFAULT NULL   COMMENT '歌单简介',
  `song_count`    INT          NOT NULL DEFAULT 0      COMMENT '歌曲数（冗余计数）',
  `play_count`    BIGINT       NOT NULL DEFAULT 0      COMMENT '播放量（冗余计数）',
  `collect_count` BIGINT       NOT NULL DEFAULT 0      COMMENT '被收藏数（冗余计数）',
  `is_public`     TINYINT      NOT NULL DEFAULT 1      COMMENT '是否公开：0-私密 1-公开',
  `status`        TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：0-封禁/隐藏 1-正常',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type_status` (`type`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '歌单表';

-- ----------------------------------------------------------------------------
-- 歌单-歌曲关联表（多对多，带排序）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `playlist_song`;
CREATE TABLE `playlist_song` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `playlist_id` BIGINT   NOT NULL                COMMENT '歌单ID -> playlist.id',
  `song_id`     BIGINT   NOT NULL                COMMENT '歌曲ID -> song.id',
  `sort`        INT      NOT NULL DEFAULT 0      COMMENT '歌曲在歌单中的顺序',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_playlist_song` (`playlist_id`, `song_id`),
  KEY `idx_song_id` (`song_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '歌单-歌曲关联表';

-- ----------------------------------------------------------------------------
-- 电台表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `radio`;
CREATE TABLE `radio` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`         VARCHAR(100) NOT NULL                COMMENT '电台名称',
  `cover`        VARCHAR(255)          DEFAULT NULL   COMMENT '电台封面路径',
  `introduction` VARCHAR(500)          DEFAULT NULL   COMMENT '电台简介',
  `play_count`   BIGINT       NOT NULL DEFAULT 0      COMMENT '播放量（冗余计数）',
  `sort`         INT          NOT NULL DEFAULT 0      COMMENT '排序值',
  `status`       TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：0-下架 1-上架',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '电台表';

-- ----------------------------------------------------------------------------
-- 电台节目表（节目音频独立于 song，也可通过 song_id 复用曲库歌曲）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `radio_program`;
CREATE TABLE `radio_program` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `radio_id`     BIGINT       NOT NULL                COMMENT '电台ID -> radio.id',
  `title`        VARCHAR(150) NOT NULL                COMMENT '节目标题',
  `song_id`      BIGINT                DEFAULT NULL   COMMENT '关联曲库歌曲ID -> song.id，可为空',
  `file_path`    VARCHAR(255)          DEFAULT NULL   COMMENT '独立音频对象路径，song_id 为空时使用',
  `duration`     INT          NOT NULL DEFAULT 0      COMMENT '时长（秒）',
  `publish_time` DATETIME              DEFAULT NULL   COMMENT '上线时间',
  `sort`         INT          NOT NULL DEFAULT 0      COMMENT '节目序号',
  `status`       TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：0-下架 1-上架',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_radio_id` (`radio_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '电台节目表';

-- ============================================================================
-- 模块二：用户模块（用户/等级/行为/评论/反馈）
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 用户等级表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `user_level`;
CREATE TABLE `user_level` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`        VARCHAR(50)  NOT NULL                COMMENT '等级名称，如 Lv1 初出茅庐',
  `level`       INT          NOT NULL                COMMENT '等级数值，从 1 递增',
  `min_exp`     BIGINT       NOT NULL DEFAULT 0      COMMENT '达到该等级所需最小成长值',
  `icon`        VARCHAR(255)          DEFAULT NULL   COMMENT '等级图标路径',
  `privilege`   VARCHAR(500)          DEFAULT NULL   COMMENT '等级特权描述',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_level` (`level`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户等级表';

-- ----------------------------------------------------------------------------
-- 用户表（门户端注册用户；后台管理员在 admin 表，二者分离）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username`        VARCHAR(50)  NOT NULL                COMMENT '登录账号',
  `password`        VARCHAR(100) NOT NULL                COMMENT '密码（BCrypt 加密）',
  `nickname`        VARCHAR(50)           DEFAULT NULL   COMMENT '昵称',
  `avatar`          VARCHAR(255)          DEFAULT NULL   COMMENT '头像路径',
  `gender`          TINYINT      NOT NULL DEFAULT 0      COMMENT '性别：0-未知 1-男 2-女',
  `birthday`        DATE                  DEFAULT NULL   COMMENT '生日',
  `phone`           VARCHAR(20)           DEFAULT NULL   COMMENT '手机号',
  `email`           VARCHAR(100)          DEFAULT NULL   COMMENT '邮箱',
  `register_channel` VARCHAR(20) NOT NULL DEFAULT 'direct' COMMENT '注册渠道 direct/search/share/activity',
  `signature`       VARCHAR(255)          DEFAULT NULL   COMMENT '个性签名',
  `level_id`        BIGINT                DEFAULT NULL   COMMENT '当前等级ID -> user_level.id',
  `exp`             BIGINT       NOT NULL DEFAULT 0      COMMENT '成长值（听歌/签到等行为累计）',
  `is_vip`          TINYINT      NOT NULL DEFAULT 0      COMMENT '是否有效会员：0-否 1-是（冗余，以 vip_expire_time 为准）',
  `vip_expire_time` DATETIME              DEFAULT NULL   COMMENT '会员到期时间',
  `status`          TINYINT      NOT NULL DEFAULT 1      COMMENT '账号状态：0-封禁 1-正常',
  `last_login_time` DATETIME              DEFAULT NULL   COMMENT '最后登录时间',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_phone` (`phone`),
  KEY `idx_email` (`email`),
  KEY `idx_level_id` (`level_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表';

-- ----------------------------------------------------------------------------
-- 用户喜欢歌曲表（"我喜欢的音乐"）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `user_like_song`;
CREATE TABLE `user_like_song` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`     BIGINT   NOT NULL                COMMENT '用户ID -> user.id',
  `song_id`     BIGINT   NOT NULL                COMMENT '歌曲ID -> song.id',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '喜欢时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_song` (`user_id`, `song_id`),
  KEY `idx_song_id` (`song_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户喜欢歌曲表';

-- ----------------------------------------------------------------------------
-- 用户收藏歌单表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `user_collect_playlist`;
CREATE TABLE `user_collect_playlist` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`     BIGINT   NOT NULL                COMMENT '用户ID -> user.id',
  `playlist_id` BIGINT   NOT NULL                COMMENT '歌单ID -> playlist.id',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_playlist` (`user_id`, `playlist_id`),
  KEY `idx_playlist_id` (`playlist_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户收藏歌单表';

-- ----------------------------------------------------------------------------
-- 最近播放表（同一用户同一歌曲仅一条，重复播放更新 play_time；应用层限制保留条数）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `recent_play`;
CREATE TABLE `recent_play` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`    BIGINT   NOT NULL                COMMENT '用户ID -> user.id',
  `song_id`    BIGINT   NOT NULL                COMMENT '歌曲ID -> song.id',
  `play_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近一次播放时间',
  `play_count` INT      NOT NULL DEFAULT 1      COMMENT '该用户对该歌曲的累计播放次数',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_song` (`user_id`, `song_id`),
  KEY `idx_user_playtime` (`user_id`, `play_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '最近播放表';

-- ----------------------------------------------------------------------------
-- 下载记录表（下载管理页数据来源，流水表）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `download_record`;
CREATE TABLE `download_record` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`     BIGINT   NOT NULL                COMMENT '用户ID -> user.id',
  `song_id`     BIGINT   NOT NULL                COMMENT '歌曲ID -> song.id',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下载时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`, `create_time`),
  KEY `idx_song_id` (`song_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '下载记录表';

-- ----------------------------------------------------------------------------
-- 评论表（多态目标：歌曲/歌单/专辑；两级结构，parent_id 指向根评论）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`       BIGINT       NOT NULL                COMMENT '评论用户ID -> user.id',
  `target_type`   TINYINT      NOT NULL                COMMENT '目标类型：1-歌曲 2-歌单 3-专辑',
  `target_id`     BIGINT       NOT NULL                COMMENT '目标ID（随 target_type 指向 song/playlist/album）',
  `content`       VARCHAR(1000) NOT NULL               COMMENT '评论内容',
  `parent_id`     BIGINT       NOT NULL DEFAULT 0      COMMENT '根评论ID，0=一级评论',
  `reply_user_id` BIGINT                DEFAULT NULL   COMMENT '被回复用户ID -> user.id',
  `like_count`    INT          NOT NULL DEFAULT 0      COMMENT '点赞数（冗余计数）',
  `status`        TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：0-屏蔽 1-正常',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_type`, `target_id`, `create_time`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '评论表';

-- ----------------------------------------------------------------------------
-- 评论点赞表（防重复点赞）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `comment_like`;
CREATE TABLE `comment_like` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`     BIGINT   NOT NULL                COMMENT '用户ID -> user.id',
  `comment_id`  BIGINT   NOT NULL                COMMENT '评论ID -> comment.id',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_comment` (`user_id`, `comment_id`),
  KEY `idx_comment_id` (`comment_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '评论点赞表';

-- ----------------------------------------------------------------------------
-- 用户反馈表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `feedback`;
CREATE TABLE `feedback` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`         BIGINT        NOT NULL                COMMENT '反馈用户ID -> user.id',
  `type`            TINYINT       NOT NULL DEFAULT 1      COMMENT '反馈类型：1-功能异常 2-产品建议 3-版权投诉 4-其他',
  `content`         VARCHAR(2000) NOT NULL                COMMENT '反馈内容',
  `images`          VARCHAR(1000)          DEFAULT NULL   COMMENT '截图路径，多个用英文逗号分隔',
  `contact`         VARCHAR(100)           DEFAULT NULL   COMMENT '联系方式',
  `status`          TINYINT       NOT NULL DEFAULT 0      COMMENT '处理状态：0-待处理 1-已处理',
  `reply`           VARCHAR(1000)          DEFAULT NULL   COMMENT '处理回复',
  `handle_admin_id` BIGINT                 DEFAULT NULL   COMMENT '处理管理员ID -> admin.id',
  `handle_time`     DATETIME               DEFAULT NULL   COMMENT '处理时间',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户反馈表';

-- ============================================================================
-- 模块三：运营模块（轮播图/公告/活动）
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 轮播图表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `banner`;
CREATE TABLE `banner` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title`       VARCHAR(100) NOT NULL                COMMENT '标题',
  `image`       VARCHAR(255) NOT NULL                COMMENT '图片路径',
  `link_type`   TINYINT      NOT NULL DEFAULT 0      COMMENT '跳转类型：0-无 1-歌曲 2-歌单 3-专辑 4-活动 5-外链',
  `link_value`  VARCHAR(255)          DEFAULT NULL   COMMENT '跳转目标：业务ID或URL',
  `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序值，越小越靠前',
  `start_time`  DATETIME              DEFAULT NULL   COMMENT '生效开始时间，空=立即',
  `end_time`    DATETIME              DEFAULT NULL   COMMENT '生效结束时间，空=长期',
  `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：0-下线 1-上线',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_status_sort` (`status`, `sort`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '轮播图表';

-- ----------------------------------------------------------------------------
-- 公告表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `announcement`;
CREATE TABLE `announcement` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title`        VARCHAR(150) NOT NULL                COMMENT '公告标题',
  `content`      TEXT         NOT NULL                COMMENT '公告内容（富文本）',
  `is_top`       TINYINT      NOT NULL DEFAULT 0      COMMENT '是否置顶：0-否 1-是',
  `admin_id`     BIGINT       NOT NULL                COMMENT '发布管理员ID -> admin.id',
  `publish_time` DATETIME              DEFAULT NULL   COMMENT '发布时间',
  `status`       TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：0-草稿/下线 1-已发布',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_status_publish` (`status`, `publish_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '公告表';

-- ----------------------------------------------------------------------------
-- 活动表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `activity`;
CREATE TABLE `activity` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title`       VARCHAR(150) NOT NULL                COMMENT '活动标题',
  `cover`       VARCHAR(255)          DEFAULT NULL   COMMENT '活动封面路径',
  `content`     TEXT                                 COMMENT '活动详情（富文本）',
  `start_time`  DATETIME     NOT NULL                COMMENT '活动开始时间',
  `end_time`    DATETIME     NOT NULL                COMMENT '活动结束时间',
  `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序值',
  `status`      TINYINT      NOT NULL DEFAULT 0      COMMENT '状态：0-未发布 1-进行中 2-已结束（定时任务流转）',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_status_time` (`status`, `start_time`, `end_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '活动表';

-- ============================================================================
-- 模块四：权限与系统模块（RBAC/操作日志/系统配置）
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 管理员表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username`        VARCHAR(50)  NOT NULL                COMMENT '登录账号',
  `password`        VARCHAR(100) NOT NULL                COMMENT '密码（BCrypt 加密）',
  `nickname`        VARCHAR(50)           DEFAULT NULL   COMMENT '姓名/昵称',
  `avatar`          VARCHAR(255)          DEFAULT NULL   COMMENT '头像路径',
  `phone`           VARCHAR(20)           DEFAULT NULL   COMMENT '手机号',
  `email`           VARCHAR(100)          DEFAULT NULL   COMMENT '邮箱',
  `status`          TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：0-禁用 1-启用',
  `last_login_time` DATETIME              DEFAULT NULL   COMMENT '最后登录时间',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '管理员表';

-- ----------------------------------------------------------------------------
-- 角色表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`        VARCHAR(50)  NOT NULL                COMMENT '角色名称',
  `code`        VARCHAR(50)  NOT NULL                COMMENT '角色编码，如 SUPER_ADMIN',
  `description` VARCHAR(255)          DEFAULT NULL   COMMENT '角色描述',
  `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色表';

-- ----------------------------------------------------------------------------
-- 权限表（菜单/按钮/接口三种粒度，树形结构）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id`   BIGINT       NOT NULL DEFAULT 0      COMMENT '父权限ID，0=顶级',
  `name`        VARCHAR(50)  NOT NULL                COMMENT '权限名称',
  `code`        VARCHAR(100) NOT NULL                COMMENT '权限编码，如 music:song:audit',
  `type`        TINYINT      NOT NULL DEFAULT 1      COMMENT '类型：1-菜单 2-按钮 3-接口',
  `path`        VARCHAR(255)          DEFAULT NULL   COMMENT '前端路由或接口路径',
  `icon`        VARCHAR(100)          DEFAULT NULL   COMMENT '菜单图标',
  `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序值',
  `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '权限表';

-- ----------------------------------------------------------------------------
-- 管理员-角色关联表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `admin_role`;
CREATE TABLE `admin_role` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `admin_id`    BIGINT   NOT NULL                COMMENT '管理员ID -> admin.id',
  `role_id`     BIGINT   NOT NULL                COMMENT '角色ID -> role.id',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_role` (`admin_id`, `role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '管理员-角色关联表';

-- ----------------------------------------------------------------------------
-- 角色-权限关联表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission` (
  `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id`       BIGINT   NOT NULL                COMMENT '角色ID -> role.id',
  `permission_id` BIGINT   NOT NULL                COMMENT '权限ID -> permission.id',
  `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
  KEY `idx_permission_id` (`permission_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色-权限关联表';

-- ----------------------------------------------------------------------------
-- 操作日志表（后台操作审计，日志表不设 update_time/deleted）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `admin_id`    BIGINT        NOT NULL                COMMENT '操作人ID -> admin.id',
  `module`      VARCHAR(50)            DEFAULT NULL   COMMENT '功能模块，如 音乐管理',
  `operation`   VARCHAR(100)           DEFAULT NULL   COMMENT '操作描述，如 审核通过歌曲',
  `method`      VARCHAR(255)           DEFAULT NULL   COMMENT '请求方法/接口路径',
  `params`      TEXT                                  COMMENT '请求参数（JSON）',
  `ip`          VARCHAR(50)            DEFAULT NULL   COMMENT '操作IP',
  `status`      TINYINT       NOT NULL DEFAULT 1      COMMENT '结果：0-失败 1-成功',
  `error_msg`   VARCHAR(1000)          DEFAULT NULL   COMMENT '失败异常信息',
  `cost_time`   BIGINT        NOT NULL DEFAULT 0      COMMENT '耗时（毫秒）',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin_id` (`admin_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '操作日志表';

-- ----------------------------------------------------------------------------
-- 系统配置表（键值对，系统设置页数据来源）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key`   VARCHAR(100)  NOT NULL                COMMENT '配置键，如 site_name',
  `config_value` VARCHAR(2000)          DEFAULT NULL   COMMENT '配置值',
  `description`  VARCHAR(255)           DEFAULT NULL   COMMENT '配置说明',
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统配置表';

-- ============================================================================
-- 模块五：交易与统计模块（会员套餐/订单/单曲购买/播放统计/榜单快照）
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 会员套餐表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `vip_package`;
CREATE TABLE `vip_package` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`           VARCHAR(50)   NOT NULL                COMMENT '套餐名称，如 月卡/季卡/年卡',
  `days`           INT           NOT NULL                COMMENT '会员时长（天）',
  `price`          DECIMAL(10,2) NOT NULL                COMMENT '售价（元）',
  `original_price` DECIMAL(10,2)          DEFAULT NULL   COMMENT '划线原价（元）',
  `sort`           INT           NOT NULL DEFAULT 0      COMMENT '排序值',
  `status`         TINYINT       NOT NULL DEFAULT 1      COMMENT '状态：0-下架 1-上架',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '会员套餐表';

-- ----------------------------------------------------------------------------
-- 订单表（统一承载会员购买与单曲购买；order 为 MySQL 保留字，故命名 order_info）
-- 状态机：0待支付 -> 1已支付 / 2超时关闭；1已支付 -> 3已退款
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `order_info`;
CREATE TABLE `order_info` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no`       VARCHAR(64)   NOT NULL                COMMENT '业务订单号（雪花/时间戳规则生成）',
  `user_id`        BIGINT        NOT NULL                COMMENT '下单用户ID -> user.id',
  `order_type`     TINYINT       NOT NULL                COMMENT '订单类型：1-会员套餐 2-单曲购买',
  `target_id`      BIGINT        NOT NULL                COMMENT '商品ID：order_type=1 指向 vip_package.id，=2 指向 song.id',
  `target_name`    VARCHAR(150)  NOT NULL                COMMENT '商品名称快照（下单时冗余，防商品改名）',
  `amount`         DECIMAL(10,2) NOT NULL                COMMENT '应付金额快照（元）',
  `pay_channel`    TINYINT                DEFAULT NULL   COMMENT '支付渠道：1-支付宝 2-微信 3-模拟支付',
  `status`         TINYINT       NOT NULL DEFAULT 0      COMMENT '订单状态：0-待支付 1-已支付 2-超时关闭 3-已退款',
  `expire_time`    DATETIME      NOT NULL                COMMENT '支付截止时间（超时任务据此关单）',
  `pay_time`       DATETIME               DEFAULT NULL   COMMENT '支付成功时间',
  `close_time`     DATETIME               DEFAULT NULL   COMMENT '关闭/退款时间',
  `transaction_id` VARCHAR(100)           DEFAULT NULL   COMMENT '第三方支付流水号',
  `remark`         VARCHAR(255)           DEFAULT NULL   COMMENT '备注',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`, `create_time`),
  KEY `idx_status_expire` (`status`, `expire_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '订单表（会员/单曲购买）';

-- ----------------------------------------------------------------------------
-- 用户已购单曲表（支付成功后写入，播放/下载鉴权直接查本表，避免扫订单表）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `user_song_purchase`;
CREATE TABLE `user_song_purchase` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`     BIGINT   NOT NULL                COMMENT '用户ID -> user.id',
  `song_id`     BIGINT   NOT NULL                COMMENT '歌曲ID -> song.id',
  `order_id`    BIGINT   NOT NULL                COMMENT '来源订单ID -> order_info.id',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '购买时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_song` (`user_id`, `song_id`),
  KEY `idx_song_id` (`song_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户已购单曲表';

-- ----------------------------------------------------------------------------
-- 歌曲每日统计表（Redis 计数定时落库，按 歌曲+日期 聚合；榜单与数据统计来源）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `play_stat_daily`;
CREATE TABLE `play_stat_daily` (
  `id`             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `song_id`        BIGINT   NOT NULL                COMMENT '歌曲ID -> song.id',
  `stat_date`      DATE     NOT NULL                COMMENT '统计日期',
  `play_count`     BIGINT   NOT NULL DEFAULT 0      COMMENT '当日播放量',
  `like_count`     BIGINT   NOT NULL DEFAULT 0      COMMENT '当日新增喜欢数',
  `collect_count`  BIGINT   NOT NULL DEFAULT 0      COMMENT '当日新增收藏数',
  `download_count` BIGINT   NOT NULL DEFAULT 0      COMMENT '当日下载量',
  `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_song_date` (`song_id`, `stat_date`),
  KEY `idx_stat_date` (`stat_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '歌曲每日统计表';

-- ----------------------------------------------------------------------------
-- 榜单快照表（定时任务基于 play_stat_daily 等计算后落库，前端直接读快照）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `rank_snapshot`;
CREATE TABLE `rank_snapshot` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rank_type`   TINYINT       NOT NULL                COMMENT '榜单类型：1-热歌榜 2-新歌榜 3-原创榜 4-飙升榜',
  `stat_date`   DATE          NOT NULL                COMMENT '榜单日期（生成日）',
  `song_id`     BIGINT        NOT NULL                COMMENT '歌曲ID -> song.id',
  `rank_no`     INT           NOT NULL                COMMENT '名次，从 1 开始',
  `score`       DECIMAL(16,2) NOT NULL DEFAULT 0.00   COMMENT '榜单得分（热度/增速等计算值）',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_date_rank` (`rank_type`, `stat_date`, `rank_no`),
  KEY `idx_type_date_song` (`rank_type`, `stat_date`, `song_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '榜单快照表';

-- ============================================================================
-- 初始化完成，共 36 张表
-- ============================================================================
