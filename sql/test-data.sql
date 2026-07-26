-- ============================================================================
-- 音域（yinyu）演示数据脚本
-- 前置条件：先执行 sql/init.sql
--
-- 密码说明：
--   所有 password 字段均为明文 "123456" 的真实 BCrypt 哈希，可直接登录。
--   仅用于演示，正式环境请用后端 BCryptPasswordEncoder.encode() 重新生成替换。
-- 文件路径说明：
--   file_path / cover 等均为 MinIO 桶内相对路径占位（music 桶 / image 桶），
--   不代表真实存在的对象，联调时替换为实际上传后的对象路径。
-- ============================================================================

SET NAMES utf8mb4;
USE yinyu;

-- ----------------------------------------------------------------------------
-- 管理员 / 角色 / 权限（RBAC）
-- ----------------------------------------------------------------------------
-- 密码统一为 123456（真实 BCrypt 哈希，生产环境请务必修改）
INSERT INTO `admin` (`id`, `username`, `password`, `nickname`, `email`, `status`) VALUES
(1, 'admin', '$2a$10$7dk/TppRakU41XguIWtqxO0ty6wWiSbC5Fj/jz8G6O24Aeu1y8rEy', '超级管理员', 'admin@yinyu.com', 1),
(2, 'auditor', '$2a$10$7dk/TppRakU41XguIWtqxO0ty6wWiSbC5Fj/jz8G6O24Aeu1y8rEy', '内容审核员', 'auditor@yinyu.com', 1);

INSERT INTO `role` (`id`, `name`, `code`, `description`, `status`) VALUES
(1, '超级管理员', 'SUPER_ADMIN', '拥有全部权限', 1),
(2, '内容审核员', 'CONTENT_AUDITOR', '负责音乐审核与内容管理', 1);

INSERT INTO `permission` (`id`, `parent_id`, `name`, `code`, `type`, `path`, `icon`, `sort`, `status`) VALUES
(1,  0, '首页看板',   'dashboard',          1, '/dashboard',      'DashboardOutlined', 1, 1),
(2,  0, '内容管理',   'content',            1, '/content',        'CustomerServiceOutlined', 2, 1),
(3,  2, '音乐管理',   'content:song',       1, '/content/song',   NULL, 1, 1),
(4,  2, '音乐审核',   'content:audit',      1, '/content/audit',  NULL, 2, 1),
(5,  4, '审核通过',   'content:audit:pass',   2, NULL, NULL, 1, 1),
(6,  4, '审核驳回',   'content:audit:reject', 2, NULL, NULL, 2, 1),
(7,  2, '歌手管理',   'content:singer',     1, '/content/singer', NULL, 3, 1),
(8,  2, '歌单管理',   'content:playlist',   1, '/content/playlist', NULL, 4, 1),
(9,  0, '用户管理',   'user',               1, '/user',           'UserOutlined', 3, 1),
(10, 0, '运营管理',   'operation',          1, '/operation',      'NotificationOutlined', 4, 1),
(11, 0, '系统管理',   'system',             1, '/system',         'SettingOutlined', 5, 1);

-- 接口级权限码（type=3，二期补齐：后端 @RequireAdmin 使用的全部 permission）
INSERT INTO `permission` (`id`, `parent_id`, `name`, `code`, `type`, `sort`, `status`) VALUES
-- 看板与统计（挂 首页看板）
(100, 1, '看板查看',     'dashboard:view',  3, 1, 1),
(101, 1, '统计查询',     'stats:view',      3, 2, 1),
(102, 1, '统计导出',     'stats:export',    3, 3, 1),
(103, 1, '榜单快照生成', 'rank:generate',   3, 4, 1),
-- 内容管理（挂 内容管理）
(110, 2, '音乐列表', 'music:list',   3, 10, 1),
(111, 2, '音乐新增', 'music:add',    3, 11, 1),
(112, 2, '音乐修改', 'music:edit',   3, 12, 1),
(113, 2, '音乐上下架', 'music:shelf', 3, 13, 1),
(114, 2, '音乐删除', 'music:delete', 3, 14, 1),
(115, 2, '音乐审核', 'music:audit',  3, 15, 1),
(116, 2, '审核通过(接口)', 'music:audit:pass',   3, 16, 1),
(117, 2, '审核驳回(接口)', 'music:audit:reject', 3, 17, 1),
(118, 2, '歌手列表', 'singer:list',   3, 20, 1),
(119, 2, '歌手新增', 'singer:add',    3, 21, 1),
(120, 2, '歌手修改', 'singer:edit',   3, 22, 1),
(121, 2, '歌手删除', 'singer:delete', 3, 23, 1),
(122, 2, '专辑列表', 'album:list',    3, 24, 1),
(123, 2, '专辑新增', 'album:add',     3, 25, 1),
(124, 2, '专辑修改', 'album:edit',    3, 26, 1),
(125, 2, '专辑删除', 'album:delete',  3, 27, 1),
(126, 2, '分类列表', 'category:list',   3, 28, 1),
(127, 2, '分类新增', 'category:add',    3, 29, 1),
(128, 2, '分类修改', 'category:edit',   3, 30, 1),
(129, 2, '分类删除', 'category:delete', 3, 31, 1),
(130, 2, '歌单列表', 'playlist:list',   3, 32, 1),
(131, 2, '歌单新增', 'playlist:add',    3, 33, 1),
(132, 2, '歌单修改', 'playlist:edit',   3, 34, 1),
(133, 2, '歌单删除', 'playlist:delete', 3, 35, 1),
(134, 2, '版权列表', 'copyright:list',   3, 36, 1),
(135, 2, '版权新增', 'copyright:add',    3, 37, 1),
(136, 2, '版权修改', 'copyright:edit',   3, 38, 1),
(137, 2, '版权删除', 'copyright:delete', 3, 39, 1),
(138, 2, '评论删除', 'comment:delete',   3, 40, 1),
-- 用户运营（挂 用户管理）
(140, 9, '用户列表', 'user:list',    3, 1, 1),
(141, 9, '用户禁用', 'user:disable', 3, 2, 1),
(142, 9, '用户重置密码', 'user:reset', 3, 3, 1),
(143, 9, '会员列表', 'vip:list',     3, 4, 1),
(144, 9, '会员时长调整', 'vip:adjust', 3, 5, 1),
(145, 9, '套餐管理', 'vip:plan',     3, 6, 1),
(146, 9, '等级查询', 'level:list',   3, 7, 1),
(147, 9, '等级维护', 'level:edit',   3, 8, 1),
(148, 9, '反馈列表', 'feedback:list',   3, 9, 1),
(149, 9, '反馈处理', 'feedback:handle', 3, 10, 1),
-- 运营管理（挂 运营管理）
(150, 10, '轮播图列表', 'banner:list',   3, 1, 1),
(151, 10, '轮播图新增', 'banner:add',    3, 2, 1),
(152, 10, '轮播图修改', 'banner:edit',   3, 3, 1),
(153, 10, '轮播图删除', 'banner:delete', 3, 4, 1),
(154, 10, '公告列表', 'notice:list',   3, 5, 1),
(155, 10, '公告发布', 'notice:add',    3, 6, 1),
(156, 10, '公告修改', 'notice:edit',   3, 7, 1),
(157, 10, '公告删除', 'notice:delete', 3, 8, 1),
(158, 10, '活动列表', 'activity:list',   3, 9, 1),
(159, 10, '活动新增', 'activity:add',    3, 10, 1),
(160, 10, '活动修改', 'activity:edit',   3, 11, 1),
(161, 10, '活动上下线', 'activity:shelf', 3, 12, 1),
(162, 10, '活动删除', 'activity:delete', 3, 13, 1),
(163, 10, '订单列表', 'order:list',   3, 14, 1),
(164, 10, '订单关闭', 'order:close',  3, 15, 1),
(165, 10, '订单退款', 'order:refund', 3, 16, 1),
-- 系统管理（挂 系统管理）
(170, 11, '管理员列表', 'system:admin:list',   3, 1, 1),
(171, 11, '管理员新增', 'system:admin:add',    3, 2, 1),
(172, 11, '管理员修改', 'system:admin:edit',   3, 3, 1),
(173, 11, '管理员重置密码', 'system:admin:reset', 3, 4, 1),
(174, 11, '管理员删除', 'system:admin:delete', 3, 5, 1),
(175, 11, '角色列表', 'system:role:list',   3, 6, 1),
(176, 11, '角色新增', 'system:role:add',    3, 7, 1),
(177, 11, '角色修改', 'system:role:edit',   3, 8, 1),
(178, 11, '角色删除', 'system:role:delete', 3, 9, 1),
(179, 11, '角色授权', 'system:role:grant',  3, 10, 1),
(180, 11, '权限树查询', 'system:perm:list', 3, 11, 1),
(181, 11, '权限维护', 'system:perm:edit',   3, 12, 1),
(182, 11, '设置查询', 'system:setting:view', 3, 13, 1),
(183, 11, '设置修改', 'system:setting:edit', 3, 14, 1),
(184, 11, '操作日志查询', 'system:log:list',  3, 15, 1);

INSERT INTO `admin_role` (`admin_id`, `role_id`) VALUES (1, 1), (2, 2);

-- 超级管理员绑定全部权限（SUPER_ADMIN 角色代码本身放行全部，绑定用于前端回显）；
-- 审核员绑定内容菜单 + 音乐列表/审核接口权限
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT 1, id FROM `permission`;
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES
(2, 2), (2, 3), (2, 4), (2, 5), (2, 6),
(2, 110), (2, 115), (2, 116), (2, 117);

-- ----------------------------------------------------------------------------
-- 用户等级 / 演示用户
-- ----------------------------------------------------------------------------
INSERT INTO `user_level` (`id`, `name`, `level`, `min_exp`, `privilege`) VALUES
(1, 'Lv1 初出茅庐', 1, 0,     '基础听歌'),
(2, 'Lv2 小有名气', 2, 500,   '专属等级徽章'),
(3, 'Lv3 崭露头角', 3, 2000,  '评论特殊标识'),
(4, 'Lv4 声名远扬', 4, 8000,  '生日专属推荐'),
(5, 'Lv5 音域达人', 5, 20000, '达人身份标识');

-- 密码统一为 123456（真实 BCrypt 哈希，生产环境请务必修改）
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `gender`, `level_id`, `exp`, `is_vip`, `vip_expire_time`, `status`, `register_channel`) VALUES
(1, 'demo_user',  '$2a$10$7dk/TppRakU41XguIWtqxO0ty6wWiSbC5Fj/jz8G6O24Aeu1y8rEy', '爱乐小明', 1, 2, 860, 1, '2026-12-31 23:59:59', 1, 'direct'),
(2, 'music_fan',  '$2a$10$7dk/TppRakU41XguIWtqxO0ty6wWiSbC5Fj/jz8G6O24Aeu1y8rEy', '深夜电台迷', 2, 1, 120, 0, NULL, 1, 'share');

-- ----------------------------------------------------------------------------
-- 分类 / 标签
-- ----------------------------------------------------------------------------
INSERT INTO `category` (`id`, `name`, `parent_id`, `sort`, `status`) VALUES
(1, '流行', 0, 1, 1),
(2, '摇滚', 0, 2, 1),
(3, '民谣', 0, 3, 1),
(4, '电子', 0, 4, 1),
(5, '古风', 0, 5, 1);

INSERT INTO `tag` (`id`, `name`, `type`, `sort`, `status`) VALUES
(1, '华语', 2, 1, 1),
(2, '治愈', 4, 2, 1),
(3, '夜晚', 3, 3, 1),
(4, '驾车', 3, 4, 1);

-- ----------------------------------------------------------------------------
-- 歌手（虚拟占位歌手，非真实人物）
-- ----------------------------------------------------------------------------
INSERT INTO `singer` (`id`, `name`, `pinyin`, `type`, `region`, `avatar`, `introduction`, `hot`, `status`) VALUES
(1, '林晚风', 'linwanfeng',  1, '内地', 'image/singer/1.jpg', '虚拟演示歌手：都市流行男歌手。', 9800, 1),
(2, '苏子衿', 'suzijin',     2, '内地', 'image/singer/2.jpg', '虚拟演示歌手：古风女歌手。', 8600, 1),
(3, '夜航西飞', 'yehangxifei', 3, '内地', 'image/singer/3.jpg', '虚拟演示乐队：独立摇滚四人组。', 7200, 1),
(4, '陈屿声', 'chenyusheng', 1, '港台', 'image/singer/4.jpg', '虚拟演示歌手：民谣创作人。', 5400, 1),
(5, '白鹭Louise', 'bailu',   2, '欧美', 'image/singer/5.jpg', '虚拟演示歌手：电子流行女歌手。', 6100, 1),
(6, '青灯客', 'qingdengke',  1, '内地', 'image/singer/6.jpg', '虚拟演示歌手：原创古风唱作人。', 4300, 1);

-- ----------------------------------------------------------------------------
-- 专辑
-- ----------------------------------------------------------------------------
INSERT INTO `album` (`id`, `name`, `singer_id`, `cover`, `publish_date`, `company`, `introduction`, `song_count`, `status`) VALUES
(1, '晚风信笺', 1, 'image/album/1.jpg', '2026-03-01', '音域唱片', '林晚风首张个人专辑。', 4, 1),
(2, '山河入梦', 2, 'image/album/2.jpg', '2026-05-20', '音域唱片', '苏子衿古风概念专辑。', 3, 1),
(3, '午夜航线', 3, 'image/album/3.jpg', '2026-01-15', '独立厂牌', '夜航西飞乐队现场精选。', 3, 1);

-- ----------------------------------------------------------------------------
-- 歌曲（file_path 为 MinIO music 桶路径占位）
-- audit_status：0-待审核 1-通过 2-驳回；演示覆盖三种审核状态
-- ----------------------------------------------------------------------------
INSERT INTO `song` (`id`, `name`, `album_id`, `category_id`, `cover`, `file_path`, `lyric_path`, `duration`, `is_original`, `pay_type`, `price`, `play_count`, `like_count`, `audit_status`, `status`, `upload_admin_id`, `publish_time`) VALUES
(1,  '晚风告白',   1, 1, 'image/song/1.jpg',  'music/song/2026/03/wanfeng-gaobai.mp3',   'music/lyric/1.lrc',  238, 0, 0, 0.00, 158000, 8200, 1, 1, 1, '2026-03-01 10:00:00'),
(2,  '城市游离',   1, 1, 'image/song/2.jpg',  'music/song/2026/03/chengshi-youli.mp3',   'music/lyric/2.lrc',  215, 0, 1, 0.00, 96000,  4100, 1, 1, 1, '2026-03-01 10:00:00'),
(3,  '入梦令',     2, 5, 'image/song/3.jpg',  'music/song/2026/05/rumengling.mp3',       'music/lyric/3.lrc',  264, 1, 0, 0.00, 132000, 9800, 1, 1, 1, '2026-05-20 10:00:00'),
(4,  '山河谣',     2, 5, 'image/song/4.jpg',  'music/song/2026/05/shanheyao.mp3',        'music/lyric/4.lrc',  247, 1, 2, 2.00, 88000,  5600, 1, 1, 1, '2026-05-20 10:00:00'),
(5,  '午夜航线',   3, 2, 'image/song/5.jpg',  'music/song/2026/01/wuye-hangxian.mp3',    'music/lyric/5.lrc',  302, 0, 0, 0.00, 76000,  3900, 1, 1, 1, '2026-01-15 10:00:00'),
(6,  '失重练习',   3, 2, 'image/song/6.jpg',  'music/song/2026/01/shizhong-lianxi.mp3',  'music/lyric/6.lrc',  289, 0, 0, 0.00, 45000,  2100, 1, 1, 1, '2026-01-15 10:00:00'),
(7,  '南方来信',   NULL, 3, 'image/song/7.jpg', 'music/song/2026/06/nanfang-laixin.mp3', 'music/lyric/7.lrc',  226, 1, 0, 0.00, 63000,  3300, 1, 1, 1, '2026-06-10 10:00:00'),
(8,  'Neon Tide',  NULL, 4, 'image/song/8.jpg', 'music/song/2026/07/neon-tide.mp3',      NULL,                198, 0, 1, 0.00, 52000,  2600, 1, 1, 1, '2026-07-01 10:00:00'),
(9,  '青灯行',     NULL, 5, 'image/song/9.jpg', 'music/song/2026/07/qingdengxing.mp3',   'music/lyric/9.lrc',  255, 1, 0, 0.00, 0,      0,    0, 0, 1, NULL),
(10, '雾中电台',   NULL, 4, 'image/song/10.jpg','music/song/2026/07/wuzhong-diantai.mp3', NULL,               211, 0, 0, 0.00, 0,      0,    2, 0, 1, NULL);

-- 歌曲-歌手（含合唱示例：歌曲7 由 陈屿声 x 林晚风 合唱，歌曲8 由 白鹭Louise x 苏子衿 合唱）
INSERT INTO `song_singer` (`song_id`, `singer_id`, `sort`) VALUES
(1, 1, 0), (2, 1, 0),
(3, 2, 0), (4, 2, 0),
(5, 3, 0), (6, 3, 0),
(7, 4, 0), (7, 1, 1),
(8, 5, 0), (8, 2, 1),
(9, 6, 0),
(10, 5, 0);

INSERT INTO `song_tag` (`song_id`, `tag_id`) VALUES
(1, 1), (1, 2), (3, 1), (3, 3), (5, 4), (7, 2), (8, 3);

-- 审核记录（对应 song 表当前状态：9 待审核无记录，10 有驳回记录）
INSERT INTO `song_audit_record` (`song_id`, `audit_status`, `reason`, `admin_id`, `create_time`) VALUES
(1, 1, '内容合规，审核通过', 2, '2026-03-01 09:30:00'),
(3, 1, '内容合规，审核通过', 2, '2026-05-20 09:30:00'),
(10, 2, '音频存在明显杂音，请重新上传后再提交审核', 2, '2026-07-20 15:00:00');

-- ----------------------------------------------------------------------------
-- 歌单（1 官方 + 2 用户自建）
-- ----------------------------------------------------------------------------
INSERT INTO `playlist` (`id`, `name`, `user_id`, `type`, `cover`, `introduction`, `song_count`, `play_count`, `collect_count`, `is_public`, `status`) VALUES
(1, '编辑精选 | 盛夏晚风', 0, 1, 'image/playlist/1.jpg', '官方运营歌单：夏夜通勤路上的温柔曲目。', 5, 32000, 1200, 1, 1),
(2, '深夜写代码专用',      1, 0, 'image/playlist/2.jpg', '安静但不困，适合专注。', 4, 860, 35, 1, 1),
(3, '古风收藏夹',          2, 0, 'image/playlist/3.jpg', '入梦山河，一人一盏青灯。', 3, 420, 12, 1, 1);

INSERT INTO `playlist_song` (`playlist_id`, `song_id`, `sort`) VALUES
(1, 1, 1), (1, 3, 2), (1, 5, 3), (1, 7, 4), (1, 8, 5),
(2, 5, 1), (2, 6, 2), (2, 8, 3), (2, 2, 4),
(3, 3, 1), (3, 4, 2), (3, 7, 3);

-- ----------------------------------------------------------------------------
-- 电台 / 电台节目（二期：电台模块演示数据，节目复用曲库歌曲）
-- ----------------------------------------------------------------------------
INSERT INTO `radio` (`id`, `name`, `cover`, `introduction`, `play_count`, `sort`, `status`) VALUES
(1, '深夜治愈电台', 'image/radio/1.jpg', '入睡前的温柔曲目，轻音乐与人声均衡混排。', 5200, 1, 1),
(2, '通勤能量电台', 'image/radio/2.jpg', '早晚高峰的节奏之选，摇滚与电子为主。', 3100, 2, 1);

INSERT INTO `radio_program` (`radio_id`, `title`, `song_id`, `duration`, `publish_time`, `sort`, `status`) VALUES
(1, '第1期 · 晚风告白', 1, 238, '2026-07-01 21:00:00', 1, 1),
(1, '第2期 · 入梦令',   3, 264, '2026-07-08 21:00:00', 2, 1),
(1, '第3期 · 南方来信', 7, 226, '2026-07-15 21:00:00', 3, 1),
(2, '第1期 · 午夜航线', 5, 302, '2026-07-05 08:00:00', 1, 1),
(2, '第2期 · 失重练习', 6, 289, '2026-07-12 08:00:00', 2, 1),
(2, '第3期 · Neon Tide', 8, 198, '2026-07-19 08:00:00', 3, 1);

-- ----------------------------------------------------------------------------
-- 歌曲版权（二期演示数据）
-- ----------------------------------------------------------------------------
INSERT INTO `song_copyright` (`id`, `song_id`, `owner`, `license_type`, `start_date`, `end_date`, `file_url`) VALUES
(1, 1, '音域唱片',   'BUYOUT',   '2026-01-01', '2028-12-31', 'image/license/1.pdf'),
(2, 3, '苏子衿工作室', 'ORIGINAL', '2026-05-01', NULL,        NULL),
(3, 5, '独立厂牌',   'LICENSE',  '2026-01-01', '2026-08-15', 'image/license/3.pdf');

-- ----------------------------------------------------------------------------
-- 用户行为演示数据
-- ----------------------------------------------------------------------------
INSERT INTO `user_like_song` (`user_id`, `song_id`) VALUES (1, 1), (1, 3), (1, 5), (2, 3), (2, 4);
INSERT INTO `user_collect_playlist` (`user_id`, `playlist_id`) VALUES (1, 1), (2, 1), (1, 3);
INSERT INTO `recent_play` (`user_id`, `song_id`, `play_time`, `play_count`) VALUES
(1, 1, '2026-07-25 22:10:00', 12), (1, 3, '2026-07-25 23:02:00', 5), (2, 4, '2026-07-24 20:30:00', 3);
INSERT INTO `download_record` (`user_id`, `song_id`) VALUES (1, 1), (1, 5);

INSERT INTO `comment` (`id`, `user_id`, `target_type`, `target_id`, `content`, `parent_id`, `reply_user_id`, `like_count`) VALUES
(1, 1, 1, 1, '前奏一响就知道是今年夏天的单曲循环了。', 0, NULL, 18),
(2, 2, 1, 1, '同感，副歌太抓耳了！', 1, 1, 6),
(3, 2, 2, 1, '官方歌单质量在线，收藏了。', 0, NULL, 3);
INSERT INTO `comment_like` (`user_id`, `comment_id`) VALUES (2, 1), (1, 2);

INSERT INTO `feedback` (`user_id`, `type`, `content`, `contact`, `status`) VALUES
(2, 1, '私人FM切歌偶尔卡顿，网络正常情况下也会出现。', 'music_fan@example.com', 0);

-- ----------------------------------------------------------------------------
-- 运营数据：轮播图 / 公告 / 活动
-- ----------------------------------------------------------------------------
INSERT INTO `banner` (`title`, `image`, `link_type`, `link_value`, `sort`, `status`) VALUES
('新专上线 | 山河入梦', 'image/banner/1.jpg', 3, '2', 1, 1),
('盛夏晚风精选歌单',     'image/banner/2.jpg', 2, '1', 2, 1),
('年卡会员限时 7 折',    'image/banner/3.jpg', 4, '1', 3, 1);

INSERT INTO `announcement` (`title`, `content`, `is_top`, `admin_id`, `publish_time`, `status`) VALUES
('音域平台上线公告', '<p>欢迎来到音域，发现你的专属声音。</p>', 1, 1, '2026-07-01 10:00:00', 1),
('7 月版本更新说明', '<p>本次更新：新增私人FM、下载管理与原创榜。</p>', 0, 1, '2026-07-15 10:00:00', 1);

INSERT INTO `activity` (`id`, `title`, `cover`, `content`, `start_time`, `end_time`, `sort`, `status`) VALUES
(1, '夏日会员狂欢季', 'image/activity/1.jpg', '<p>活动期间购买年卡会员享 7 折优惠。</p>', '2026-07-10 00:00:00', '2026-08-10 23:59:59', 1, 1);

-- ----------------------------------------------------------------------------
-- 交易与统计：会员套餐 / 订单 / 已购单曲 / 日统计 / 榜单快照
-- ----------------------------------------------------------------------------
INSERT INTO `vip_package` (`id`, `name`, `days`, `price`, `original_price`, `sort`, `status`) VALUES
(1, '月卡', 31, 15.00, 18.00, 1, 1),
(2, '季卡', 93, 40.00, 54.00, 2, 1),
(3, '年卡', 366, 128.00, 216.00, 3, 1);

INSERT INTO `order_info` (`order_no`, `user_id`, `order_type`, `target_id`, `target_name`, `amount`, `pay_channel`, `status`, `expire_time`, `pay_time`, `transaction_id`) VALUES
('YY202607100001', 1, 1, 3, '年卡', 128.00, 3, 1, '2026-07-10 12:30:00', '2026-07-10 12:05:00', 'MOCK-PAY-0001'),
('YY202607220002', 2, 2, 4, '山河谣', 2.00, 3, 0, '2026-07-26 23:59:00', NULL, NULL),
('YY202607180003', 2, 1, 1, '月卡', 15.00, NULL, 2, '2026-07-18 12:30:00', NULL, NULL);

INSERT INTO `user_song_purchase` (`user_id`, `song_id`, `order_id`) VALUES (1, 4, 1);

INSERT INTO `play_stat_daily` (`song_id`, `stat_date`, `play_count`, `like_count`, `collect_count`, `download_count`) VALUES
(1, '2026-07-24', 5200, 210, 96, 45),
(1, '2026-07-25', 6100, 260, 120, 52),
(3, '2026-07-24', 4300, 300, 150, 30),
(3, '2026-07-25', 7800, 420, 210, 41),
(5, '2026-07-25', 2100, 80, 40, 12);

INSERT INTO `rank_snapshot` (`rank_type`, `stat_date`, `song_id`, `rank_no`, `score`) VALUES
(1, '2026-07-25', 1, 1, 96500.00),
(1, '2026-07-25', 3, 2, 91200.00),
(1, '2026-07-25', 5, 3, 65400.00),
(2, '2026-07-25', 8, 1, 52000.00),
(3, '2026-07-25', 3, 1, 88000.00),
(4, '2026-07-25', 3, 1, 181.40);

-- ----------------------------------------------------------------------------
-- 系统配置
-- ----------------------------------------------------------------------------
INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('site_name', '音域', '站点名称'),
('site_logo', 'image/system/logo.png', '站点Logo路径'),
('order_expire_minutes', '15', '订单待支付超时时间（分钟）'),
('recent_play_limit', '100', '最近播放每用户最大保留条数'),
('audit_auto_pass', '0', '歌曲是否免审自动通过：0-否 1-是'),
('pay.mock', 'true', '是否开启模拟支付渠道'),
('trial.seconds', '60', '游客试听秒数'),
('download.quota.normal', '10', '普通用户每日下载配额'),
('download.quota.vip', '100', 'VIP用户每日下载配额'),
('minio.presign.expire', '1800', '预签名URL有效期（秒）');

-- ============================================================================
-- 演示数据插入完成
-- ============================================================================
