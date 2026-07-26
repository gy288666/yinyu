# 音域 YINYU 后端（backend）

音域音乐平台 Spring Boot 3 后端工程（第一期核心）。接口契约见 `docs/api.md`，库表见 `sql/init.sql`（36 张表，实体与 Mapper 已全部生成）。

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
└── job/PlayCountSyncJob         # 每 5 分钟 Redis 播放计数回写 song.play_count 与 play_stat_daily
```

## 本地启动

1. 依赖：MySQL 8（root/gy288666，先执行 `sql/init.sql` + `sql/test-data.sql`）、Redis 7（`redis-server --daemonize yes`）、可选 MinIO（`http://localhost:9000`，minioadmin/yinyu@minio123，桶 music/cover/avatar/banner）。
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

## 第二期待实现清单

- 认证补全：refresh token rotation（10008）、修改资料/密码、上传头像
- 评论模块（发表/回复/点赞/删除、敏感词 40001）
- 会员与订单：套餐/下单/模拟支付与支付宝回调/订单查询/已购单曲（pay.mock 开关已就绪）
- 下载模块（配额 30003、附件式预签名、下载记录）
- 推荐/私人FM/电台
- 运营内容：轮播图/公告/活动（门户展示 + 后台管理）
- 专辑收藏（需新增 user_collect_album 表）、歌单标签关系
- 后台：官方歌单管理、版权管理、用户运营（用户/会员/等级/反馈）、订单管理、RBAC 管理页接口（管理员/角色/权限 CRUD 与授权）、系统设置、操作日志切面、统计查询与导出、实时动态、批量导入/批量审核
- 榜单快照生成定时任务（当前读取已有快照）、音频时长/码率真实解析（引入解析库）

## 已知取舍

- 歌词接口返回 .lrc 文件访问地址而非文本（对象存储中无真实文件，联调后可改为读取内容返回）
- 歌单 tags 恒为空数组（无关系表）；`/api/my/playlists` 首位返回虚拟内置歌单（id=0，曲目走 /api/likes/songs）
- 上传音频的 duration/bitrate 为估算值，创建歌曲时可显式传入 duration
