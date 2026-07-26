# 音域 YINYU — 管理后台（web-admin）

面向运营/管理员的管理后台前端，技术栈：Vite 5 + React 18（JavaScript）+ Ant Design 5 + react-router-dom v6 + axios + ECharts（echarts-for-react）。

## 启动步骤

```bash
cd web-admin

# 1. 安装依赖（npm 源 registry.npmjs.org）
npm install

# 2. 开发模式（默认 http://localhost:5174，/api 请求代理到 http://localhost:8080）
npm run dev

# 3. 生产构建
npm run build

# 4. 本地预览构建产物
npm run preview
```

## 页面清单

| 路由 | 页面 | 说明 |
| --- | --- | --- |
| `/login` | 登录页 | 账号密码 + 图形验证码（验证码接口不可用时自动降级），token 存 localStorage |
| `/` | 数据看板 | 5 个统计卡片（含较昨日涨幅）、播放量趋势面积图（今日/近7日/近30日）、用户来源渠道环形图、实时动态、热门歌曲 TOP5、系统公告、快捷操作、音乐审核待办（三态 Tab + 行内通过/驳回） |
| `/content/music` | 音乐管理 | 搜索/分类/状态筛选、封面缩略图、上下架、新增/编辑（音频上传 + 封面上传）、删除 |
| `/content/audit` | 音乐审核 | 待审核/已通过/已驳回 Tab，试听、通过、驳回（驳回理由必填 ≥5 字） |
| `/content/playlist` | 歌单管理 | 官方歌单 CRUD（标签、推荐、置顶） |
| `/content/album` | 专辑管理 | CRUD（封面、歌手、发行日期） |
| `/content/singer` | 歌手管理 | CRUD（头像上传、地区、类型） |
| `/content/category` | 分类管理 | 两级树形 CRUD（排序、启用开关） |
| `/content/copyright` | 版权管理 | CRUD（授权类型、起止日期、到期状态标记） |
| `/user/list` | 用户列表 | 多条件查询、详情、启用/停用、重置密码 |
| `/user/vip` | 会员管理 | 会员套餐卡片展示 + VIP 用户列表 + 调整 VIP 时长 |
| `/user/level` | 用户等级 | Lv1-Lv10 播放时长阈值编辑保存 |
| `/user/feedback` | 用户反馈 | 类型/状态筛选、回复、关闭 |
| `/operation/banner` | 轮播图管理 | CRUD（图片上传、跳转类型联动、有效期、启用开关） |
| `/operation/notice` | 公告管理 | CRUD（富文本内容、生效时间） |
| `/operation/activity` | 活动管理 | CRUD + 上线/下线，过期自动标记"已结束" |
| `/operation/stats` | 数据统计 | 播放量/注册量/收益按日/周/月趋势图 + 今日热门榜单图 + 导出 CSV |
| `/system/admin` | 管理员管理 | CRUD + 分配角色 + 重置密码，内置 admin 不可停用/删除 |
| `/system/role` | 角色管理 | CRUD + 权限树勾选授权 |
| `/system/permission` | 权限管理 | 菜单/按钮权限树形维护 |
| `/system/setting` | 系统设置 | 键值对表单（试听时长、下载配额、模拟支付等） |
| `/system/log` | 操作日志 | 操作人/模块/时间范围查询 |

## 与后端联调说明

- 接口契约以 `docs/api.md` 为准：Base Path `/api`，统一响应体 `{ code, message, data }`。
- **成功码兼容**：`docs/api.md` 定义 `code=0` 为成功，前端拦截器同时接受 `code === 0` 与 `code === 200`，无需改动即可对接两种实现。
- **认证**：登录调 `POST /api/admin/auth/login`（带 `captchaKey`/`captchaCode`，验证码来自 `GET /api/admin/auth/captcha`；验证码接口 404/异常时前端降级为仅账号密码提交）。`accessToken` 存 localStorage，axios 请求拦截器统一附加 `Authorization: Bearer <token>`。
- **失效处理**：HTTP 401 或业务码 `10007` 时清除本地凭证并跳转 `/login`；`code !== 0/200` 统一 `message.error` 弹出后端 message；403/404/5xx/网络异常均有兜底提示，页面不白屏。
- **代理**：`vite.config.js` 已配置 `server.proxy`，开发模式下 `/api` 转发至 `http://localhost:8080`（后端服务地址变更时改此处即可）。
- **分页**：列表查询统一 `pageNum`（从 1 起）/ `pageSize`，响应取 `data.list` / `data.total`；接口直接返回数组时（分类树、权限树、角色、等级等）也已兼容。
- **文件上传**：封面/图片走 `POST /api/admin/upload/image`（歌手头像走 `POST /api/admin/singers/upload`），音频走 `POST /api/admin/songs/upload`（返回 `objectKey` 随歌曲表单提交），均为 `multipart/form-data` 字段 `file`。
- **兜底展示**：看板各区块用独立静默请求，任一接口缺失/报错时展示 0 或空态，不影响其他区块；列表接口失败时表格展示错误空态并可点"刷新"重试。
- **演示兜底图**：`public/img/singer/` 内置了 `resource/static/img/singer` 的歌手图片，作为无封面/头像时的演示兜底（构建时随产物输出，路径 `/img/singer/*.jpg`）。
- **CSV 导出**：数据统计页"导出 CSV"直接 `window.open` 打开 `GET /api/admin/stats/export`，需后端支持从 Cookie/查询串鉴权或允许该接口匿名下载，否则可能被 401 拦截（见"遗留问题"）。
