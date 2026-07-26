# 音域 YINYU · 在线音乐平台

一个前后端分离的在线音乐平台练手项目：面向用户的暗色金主题音乐门户 + 面向运营的管理后台。

## 技术栈

| 端 | 技术 |
|---|---|
| 后端 `backend/` | Spring Boot 3 · Java 17 · MyBatis-Plus · MySQL 8 · Redis · MinIO · JWT（用户/管理员双体系）|
| 管理后台 `web-admin/` | React 18 · Vite · Ant Design 5 · ECharts（端口 5173）|
| 用户门户 `web-portal/` | React 18 · Vite · Zustand · 手写 CSS 暗色金主题（端口 5174）|

## 目录结构

```
├── backend/       # Spring Boot 后端（约 160+ REST 接口）
├── web-admin/     # 管理后台前端（21 个路由页面）
├── web-portal/    # 用户门户前端（含全局播放器、正在播放页）
├── sql/           # init.sql 建表脚本（37 张表）+ test-data.sql 演示数据
├── docs/          # 需求文档、接口文档、数据库设计（含 ER 图）
├── deploy/        # MinIO / MySQL 部署脚本与说明
└── resource/      # 歌手演示图片（来源与授权见目录内 README）
```

## 本地启动（Windows 开发机）

1. **MySQL 8**：root 密码 `gy288666`，依次执行：
   ```bash
   mysql -uroot -pgy288666 < sql/init.sql
   mysql -uroot -pgy288666 < sql/test-data.sql
   ```
   详见 `deploy/mysql/README.md`。
2. **Redis**：本机启动 redis-server（默认 6379 无密码）。
3. **MinIO**（可选）：`deploy/minio/` 提供 docker-compose 与免 Docker 一键脚本；不启动时后端自动降级为本地目录存储（`backend/storage/`），接口不受影响。音频素材放入 `E:\yinyu-music\resource\static\music` 后经管理后台上传。
4. **后端**：`cd backend && mvn spring-boot:run`（端口 8080）。
5. **管理后台**：`cd web-admin && npm install && npm run dev` → http://localhost:5173
6. **用户门户**：`cd web-portal && npm install && npm run dev` → http://localhost:5174

## 演示账号（密码均为 123456）

| 账号 | 说明 |
|---|---|
| `admin` | 超级管理员（管理后台）|
| `auditor` | 内容审核员，仅内容/审核权限（验证 RBAC）|
| `demo_user` | 门户 VIP 用户 |
| `music_fan` | 门户普通用户 |

## 核心设计

- **播放链路**：前端播放前调 `GET /api/songs/{id}/url`，后端在此校验权益（免费/VIP/单曲已购，游客 60 秒试听）、签发 MinIO 预签名地址（或降级静态路径）并做 Redis 播放计数埋点；每 5 分钟定时回写 `song.play_count` 与 `play_stat_daily`。
- **全局播放器**：门户全局唯一 `<audio>` 挂在 App 层，Zustand 管理队列/模式/进度/音量，路由切换不断播，状态持久化 localStorage。
- **排行榜**：每日 00:30 定时任务生成 `rank_snapshot` 四榜快照（热歌/新歌/原创/飙升），接口只读快照并对比上期输出升降趋势。
- **支付**：`pay.mock=true` 时模拟支付直接发放权益（开通 VIP / 写入单曲购买）；支付宝渠道为占位，接入沙箱后替换。
- **RBAC**：菜单/按钮/接口三级权限，`@RequireAdmin(permission=...)` 注解鉴权，角色授权即时清理 Redis 权限缓存；后台写操作经 AOP 自动记录操作日志。

## 文档

- `docs/requirements.md` — 需求文档（68 条功能需求）
- `docs/api.md` — 接口文档（全部 REST 契约与错误码）
- `docs/database-design.md` — 数据库设计（分模块 ER 图 + 37 张表字段说明）
- 各子工程 README — 模块结构与联调说明

## 待完善（二期后遗留）

- 支付宝沙箱真实接入（当前 MOCK）；refresh token 轮换；用户资料编辑/头像上传
- 推荐为标签+热度伪推荐；搜索为 LIKE 实现（可演进 Elasticsearch）
- 歌单标签、专辑收藏、批量审核导入

> 本项目仅供学习演示。音频素材须使用合规纯音乐；歌手图片来源与授权见 `resource/static/img/singer/README.md`。
