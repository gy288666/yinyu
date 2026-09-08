# 音域 YINYU 后端（backend）

音域音乐平台 Spring Boot 3 后端工程（第一期核心 + 第二期扩展均已完成并联调）。接口契约见 `docs/api.md`，库表见 `sql/init.sql`（37 张表：36 张一期 + 二期新增 `song_copyright`，实体与 Mapper 已全部生成）。

## 技术栈

- Java 17（编译目标）/ Spring Boot 3.3.5 / spring-boot-starter-web + validation
- MyBatis-Plus 3.5.9（分页插件、逻辑删除、create_time/update_time 自动填充）
- MySQL 8.0（库 yinyu）、Redis 7（计数埋点/缓存/黑名单/验证码）
- MinIO Java SDK（预签名 URL；未启动时自动降级）
- JJWT 0.12（用户/管理员双 token 体系）、spring-security-crypto（仅 BCrypt）

## 模块结构

```
backend/src/main/java/com/yinyu
├── YinyuApplication.java        # 启动类（@EnableScheduling）
├── common
│   ├── result                   # Result{code,message,data} / PageResult 统一分页
│   ├── exception                # BizException + GlobalExceptionHandler（401/403/404/500 映射）
│   └── constant                 # ErrorCode（对齐 api.md 错误码表）/ RedisKeys
├── config                       # MyBatis-Plus、Jackson 时间格式、Web(拦截器/CORS/静态映射)、JWT/MinIO 配置
├── security
│   ├── JwtUtil                  # 双密钥签发校验：USER / ADMIN token 互不通用
│   ├── @RequireUser / @RequireAdmin(permission="xxx")  # 鉴权注解
│   ├── AuthInterceptor          # 统一拦截器（软登录 + 强校验 + RBAC）
│   ├── AdminPermissionService   # RBAC：admin_role/role_permission/permission，Redis 缓存 30 分钟
│   └── TokenBlacklistService    # 登出黑名单
├── util/MinioUtil               # 上传/删除/预签名；MinIO 不可用时降级（本地落盘 + 静态路径拼接）
├── entity / mapper              # 36 张表全量 Entity + Mapper（MyBatis-Plus BaseMapper）
├── dto / vo                     # 请求/响应对象
├── service                      # 业务逻辑（见下）
├── controller / controller.admin# 门户 / 后台接口
├── aop/OperationLogAspect       # 后台写操作日志切面（POST/PUT/DELETE 自动入 operation_log）
└── job                          # PlayCountSyncJob（5 分钟计数回写）/ RankSnapshotJob（每日榜单快照+启动补偿）/ OrderTimeoutJob（每分钟超时关单）
```

## 本地启动

1. 依赖：MySQL 8（root，密码经环境变量 `MYSQL_ROOT_PASSWORD` 提供，先执行 `sql/init.sql` + `sql/test-data.sql`）、Redis 7（`redis-server --daemonize yes`）、可选 MinIO（`http://localhost:9000`，账号/密码经 `MINIO_ROOT_USER`/`MINIO_ROOT_PASSWORD` 提供，桶 music/cover/avatar/banner）。
2. 注意：`sql/test-data.sql` 中的 BCrypt 哈希为占位值，需将 admin/user 的 password 更新为真实 `BCrypt("123456")` 哈希后方可用 123456 登录。
3. 本期在 `user` 表补充了一列（api.md 注册渠道与看板渠道分布依赖）：
   `ALTER TABLE user ADD COLUMN register_channel VARCHAR(20) NOT NULL DEFAULT 'direct';`
4. 构建与启动：

```bash
cd backend
mvn package -DskipTests
java -jar target/yinyu-backend.jar    # 端口 8080
```

5. MinIO 未启动时自动降级：播放/图片地址返回 `minio.static-base` 配置的静态路径拼接（`/static/{bucket}/{objectKey}`，由本服务托管本地 `storage/` 目录），上传落盘到 `storage/`，接口不 500。

## 已实现接口（第一期）

- 认证：`POST /api/auth/register|login|logout`、`GET /api/auth/me`；管理员 `GET /api/admin/auth/captcha`、`POST /api/admin/auth/login|logout`、`GET /api/admin/auth/me`（含角色/权限码/动态菜单；验证码存 Redis 5 分钟，一次性；连续错 5 次锁 10 分钟）
- 歌曲：`GET /api/songs`（分类/歌手/vip/quality/sort 筛选）、`/api/songs/newest`、`/api/songs/{id}`、`/api/songs/{id}/lyric`、核心 `GET /api/songs/{id}/url`（上架校验 20003 → 权益校验：VIP 曲 30001 / 付费曲查 user_song_purchase 30002 → 预签名或降级 URL → Redis 30 秒去重埋点 `play:count:{songId}` INCR + `rank:day:{yyyyMMdd}` ZINCRBY → 登录用户自动写最近播放；游客普通曲 trialSeconds=60）
- 歌手：`GET /api/singers`（area/type/initial）、`/{id}`、`/{id}/songs`、`/{id}/albums`
- 专辑：`GET /api/albums`、`/{id}`、`/{id}/songs`
- 歌单：`GET /api/playlists`（广场）、`/hot`、`/{id}`（私密校验 20006）；我的歌单 `GET/POST /api/my/playlists`、`PUT/DELETE /api/my/playlists/{id}`、`POST .../{id}/songs`、`DELETE .../{id}/songs/{songId}`（幂等，返回 songCount）
- 分类：`GET /api/categories`（两级树，仅启用）
- 搜索：`GET /api/search`（song/singer/album，LIKE 实现，关键词入 Redis 热搜 ZSET）、`/api/search/hot`、`/api/search/suggest`
- 排行榜：`GET /api/ranks`（四榜 top3）、`GET /api/ranks/{HOT|NEW|ORIGINAL|SOAR}`（读 rank_snapshot 最新快照，对比上期计算 trend/trendDelta）
- 喜欢/收藏：`POST/DELETE /api/likes/songs/{songId}`、`GET /api/likes/songs`；`POST/DELETE /api/collections/playlists/{id}`、`GET /api/collections?type=playlist`（失效项 invalid=true）
- 最近播放：`GET /api/recent`、`DELETE /api/recent`（写入由播放地址接口自动完成，保留 200 条）
- 后台看板：`GET /api/admin/dashboard/summary|play-trend|user-channels|hot-songs`（今日播放量取 Redis 日榜实时值；趋势读 play_stat_daily；渠道分布聚合 user.register_channel；TOP5 取今日 ZSET）
- 后台音乐：`GET/POST /api/admin/songs`、`POST /api/admin/songs/upload`（mp3/flac/wav，MinIO music 桶，multipart 100MB 上限）、`PUT /api/admin/songs/{id}`、`PUT .../{id}/status`（仅审核通过可上下架 20004）、`DELETE .../{id}`（逻辑删除）
- 后台审核：`GET /api/admin/audits`（PENDING/PASSED/REJECTED）、`PUT /api/admin/audits/{songId}/pass|reject`（写 song_audit_record；重复审核 20004；驳回原因 ≥5 字）、`GET .../{songId}/url`（试听，不埋点）
- 后台内容：歌手/专辑/分类 CRUD（被引用删除返回 20005）
- 定时任务：每 5 分钟将 `play:count:*` 增量 GETDEL 回写 `song.play_count` 与 `play_stat_daily`（失败回补重试）

### 鉴权说明

- 两套 JWT（不同密钥 + userType 声明）：用户 token 调 `/api/admin/**` 返回 403/40301，反之亦然；无 token/失效返回 401/10007。
- `@RequireAdmin(permission="music:audit")` 查 RBAC 三表并缓存 Redis；`SUPER_ADMIN` 角色放行全部权限码。

## 已实现接口（第二期）

### 门户侧

- 评论：`GET/POST /api/comments`（一级评论分页 + replies 回复列表；回复的回复挂根评论下并带 replyTo；敏感词 40001）、`DELETE /api/comments/{id}`（本人，根评论级联删回复）、`POST/DELETE /api/comments/{id}/like`（comment_like 防重复，幂等，返回 likeCount）；维护 song.comment_count
- 歌词：`GET /api/songs/{id}/lyric` 改为返回歌词文本内容（MinIO 读取，不可用降级读本地 `storage/`；文件不存在返回 code=0、`data.content=""` 并带 `hint` 提示字段；兼容保留 `lyric` 字段）；`GET /api/songs/{id}` 详情 lyric 同步改为文本
- 下载：`GET /api/songs/{id}/download-url`（权益校验同播放；每日配额普通 10 / VIP 100 超限 30003，配额读 system_config `download.quota.*`；附件式预签名 600 秒；写 download_record + song.download_count）、`GET /api/downloads` 下载记录分页
- 会员与订单：`GET /api/vip/plans`；`POST /api/orders`（VIP 套餐 / SONG 单曲两类；同商品待支付订单直接返回原单；已购单曲重复下单 20004）；`POST /api/orders/{orderNo}/pay`（MOCK 渠道 `pay.mock=true` 时直接支付成功：更新订单 + 开通 VIP 累加 `user.vip_expire_time`/`is_vip` 或写 user_song_purchase；ALIPAY 未配置沙箱密钥，返回占位 payUrl+提示）；`POST /api/orders/alipay/notify`（幂等，纯文本 success/failure，本环境跳过验签）；`GET /api/orders`、`GET /api/orders/{orderNo}`、`GET /api/purchases/songs`
- 推荐：`GET /api/recommend/daily`（标签偏好+热度+按日随机种子的伪推荐，Redis 当日缓存，游客热门版）、`GET /api/recommend/songs`、`GET /api/recommend/playlists`；私人FM `GET /api/fm/next`（随机未听过的已上架歌曲，排除 7 天内不喜欢）、`POST /api/fm/dislike`（Redis ZSET 过期时间戳）
- 电台：`GET /api/radios`、`/api/radios/{id}`（含节目数）、`/api/radios/{id}/programs`、`/api/radios/{id}/next`（Redis 已播集合一轮不重复）
- 运营展示：`GET /api/banners`（启用+有效期内按 sort）、`GET /api/notices`、`/api/notices/{id}`、`GET /api/activities`（ONGOING/ENDED）、`/api/activities/{id}`
- 反馈：`POST /api/feedbacks`（BUG/SUGGEST/COPYRIGHT/OTHER，敏感词 40001，images ≤3）、`GET /api/feedbacks`（含后台回复与状态 PENDING/REPLIED/CLOSED）

### 后台侧

- 用户管理：`GET /api/admin/users`（keyword/status/vip 筛选）、`/{id}` 详情、`PUT /{id}/status`（停用写 Redis `user:disabled:*` 标记，已签发 token 即时失效返回 10005）、`PUT /{id}/password/reset`（返回随机密码）
- 会员管理：`GET /api/admin/vips`、`PUT /api/admin/vips/{userId}`（deltaDays 可正可负）；套餐 `GET/POST/PUT/DELETE /api/admin/vip-plans`（被待支付订单引用不可删 20005）
- 订单管理：`GET /api/admin/orders`（orderNo/userId/status/时间范围）、`/{orderNo}` 详情、`PUT /{orderNo}/close`（待支付关单）、`PUT /{orderNo}/refund`（已支付退款并回收权益）
- 等级：`GET/POST /api/admin/levels`、`PUT/DELETE /api/admin/levels/{id}`（被用户引用不可删）；反馈：`GET /api/admin/feedbacks`（type/status 筛选）、`PUT /api/admin/feedbacks/{id}`（reply 置 REPLIED / status=CLOSED 关闭）
- 运营内容：轮播图/公告/活动 CRUD（`/api/admin/banners|notices|activities`，活动含 `PUT /{id}/status` 上下线）、`POST /api/admin/upload/image`（jpg/png ≤5MB，banner 桶或降级本地）
- 歌单管理：`GET/POST /api/admin/playlists`、`GET/PUT/DELETE /{id}`、`PUT /{id}/songs`（songIds 有序整体替换，加曲/移曲/排序一并覆盖）
- 版权管理：`GET/POST /api/admin/copyrights`、`PUT/DELETE /{id}`（新增 `song_copyright` 表；songId/owner/licenseType/startDate/endDate/fileUrl；expiringSoon=30 天内到期）
- RBAC 管理：管理员 CRUD+分配角色+重置密码（内置 admin 不可停用/删除 20004）；角色 CRUD+授权 `PUT /api/admin/roles/{id}/permissions`（角色列表返回 `permissionIds` 供前端回显，授权后成员权限缓存即时失效）；权限树 `GET/POST /api/admin/permissions`、`PUT/DELETE /{id}`（有子节点不可删 20005）
- 数据统计：`GET /api/admin/stats`（metric=play/register/revenue × granularity=day/week/month，跨度 ≤1 年）、`GET /api/admin/stats/export`（CSV 附件流，带 UTF-8 BOM；除 Authorization 头外支持 `?token=` query 参数鉴权，供 web-admin window.open 下载——拦截器统一支持）、`POST /api/admin/ranks/generate`（榜单快照手动触发）
- 操作日志：AOP 切面拦截 `controller.admin` 包全部 POST/PUT/DELETE，自动记录 模块/操作/操作人/IP/参数/耗时/结果 入 operation_log；`GET /api/admin/logs`（adminName/module/时间范围筛选）
- 系统设置：`GET/PUT /api/admin/settings`（system_config 键值对 upsert，60 秒本地缓存写后失效）
- 看板补充：`GET /api/admin/dashboard/events` 实时动态（注册/支付订单/审核动作聚合按时间倒序）；审核列表 `GET /api/admin/audits` status 兼容 `APPROVED`（等价 PASSED）/REJECTED

### 定时任务（二期新增）

- 榜单快照：每日 00:30 生成四榜写 rank_snapshot（HOT=累计播放、NEW=近 30 天上架、ORIGINAL=原创播放量、SOAR=近两日 play_stat_daily 增量），写入前清理当日旧快照；启动时当日无快照自动补生成；支持后台手动触发
- 订单超时：每分钟扫描超过 expire_time（下单后 `order_expire_minutes` 分钟，默认 15）的待支付订单置超时关闭

### 权限码

二期为全部后台接口挂上 `@RequireAdmin(permission=...)`，共 74 个接口级权限码（music/singer/album/category/playlist/copyright/comment/user/vip/level/feedback/banner/notice/activity/order/system:*/stats/dashboard/rank），已补进 `sql/test-data.sql` 的 permission 表 INSERT（id 100-184，type=3 接口，挂到对应菜单节点下）并同步至库；SUPER_ADMIN 绑定全部（角色代码本身也放行全部），CONTENT_AUDITOR 额外绑定 music:list 与 music:audit* 用于 RBAC 验证。

## 二期剩余取舍

- 认证补全未做：refresh token rotation（10008）、修改资料/密码、上传头像仍为三期项
- 支付宝渠道未真实接入（无沙箱密钥）：`pay` 接口 ALIPAY 返回占位 payUrl 与提示；`/api/orders/alipay/notify` 未验签（有密钥后补 SDK 验签即可），联调走 MOCK 渠道
- 推荐为"标签偏好+热度+按日随机种子"的伪推荐；FM 的"未听过"基于 recent_play 全量排除，曲库大时应改抽样
- 歌单 tags 仍为空数组（无歌单-标签关系表）；官方歌单 recommended/top 字段未落库（表无对应列）
- 专辑收藏（user_collect_album）、批量导入/批量审核、等级次日重算任务未实现
- 用户停用即时生效通过 Redis 标记（TTL 7 天）实现，仅拦截 @RequireUser 接口；歌词/音频对象存储中无真实文件时相关 URL 为降级路径
- 操作日志的"操作名"由方法名映射（新增/修改/删除/审核通过等），未细化到每接口自定义文案

## 已知取舍（一期遗留）

- `/api/my/playlists` 首位返回虚拟内置歌单（id=0，曲目走 /api/likes/songs）
- 上传音频的 duration/bitrate 为估算值，创建歌曲时可显式传入 duration
