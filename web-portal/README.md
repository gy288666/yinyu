# 音域 YINYU — 用户门户前端（web-portal）

面向听歌用户的门户站点。技术栈：Vite + React 18 + JavaScript + react-router-dom v6 + axios + zustand，UI 为手写 CSS（暗色 + 金色主题，无组件库）。

## 启动步骤

```bash
cd web-portal
npm install
npm run dev        # 开发模式，端口 5174（避免与 web-admin 冲突）
```

- 开发代理：`vite.config.js` 中已配置 `server.proxy`，将 `/api` 转发到 `http://localhost:8080`（后端服务）。
- 生产构建：`npm run build`，产物在 `dist/`；本地预览 `npm run preview`（端口 5175）。

## 页面清单

| 路由 | 页面 | 说明 |
| --- | --- | --- |
| `/` | 发现页 | 轮播横幅（/api/banners）、五个功能入口卡、为你推荐歌单、热门歌单 TOP5、排行榜卡（四榜 tab + 行内喜欢）、新歌速递横向列表 |
| `/ranks`、`/ranks/:type` | 排行榜 | HOT/NEW/ORIGINAL/SOAR 四榜 tab，支持整榜播放；每行带升降角标（↑红 / ↓绿 / －灰，来自接口 trend/trendDelta） |
| `/playlists` | 歌单广场 | 标签（分类树叶子）+ 最热/最新筛选、分页；`?hires=1` 为 Hi-Res 专区（quality=lossless 歌曲列表） |
| `/playlist/:id` | 歌单详情 | 封面头图、标签/简介、播放全部、收藏/取消收藏、歌曲列表（创建者可移除歌曲）、评论区；`?autoplay=1` 进入即播 |
| `/singers` | 歌手列表 | 地区/类型/首字母筛选 + 分页 |
| `/singer/:id` | 歌手详情 | 头像（`public/static/img/singer/` 本地图片兜底）、热门歌曲、专辑 tab |
| `/album/:id` | 专辑详情 | 专辑信息 + 曲目列表 |
| `/search?keyword=&type=` | 搜索结果 | 歌曲/歌手/专辑三 tab，无关键字时展示热搜词 |
| `/login`、`/register` | 登录/注册 | 暗色独立页 |
| `/my/likes` | 我喜欢的音乐 | 分页 + 播放全部 |
| `/my/recent` | 最近播放 | 分页 + 清空 |
| `/my/playlists` | 我的歌单 | 列表 + 创建/编辑/删除（弹窗），内置"我喜欢的音乐"不可删改 |
| `/my/downloads` | 下载管理 | 下载记录分页（/api/downloads），每行可重新下载 |
| `/vip` | 会员中心 | 套餐卡片（/api/vip/plans）、模拟购买（创建订单 → MOCK 支付；ALIPAY 时打开 payUrl 并轮询订单状态） |
| `/recommend/daily` | 每日推荐 | 复用 /api/recommend/daily 的列表页 |
| `/fm` | 私人FM | 极简黑胶页：播放/暂停、下一首、喜欢、不喜欢（FM 模式播放器隐藏"上一首"） |
| `/radios` | 电台 | 电台列表，点击调 /api/radios/{id}/next 开始播放 |
| `/playing` | 正在播放 | 点击底部播放条封面/歌名进入：黑胶封面旋转（播放转动/暂停停止）、LRC 歌词滚动高亮（/api/songs/{id}/lyric，无歌词显示"纯音乐，请欣赏"）、喜欢/下载、歌曲评论区 |

整体布局：顶部导航（logo/主导航/搜索框/头像或登录按钮）+ 左侧边栏（常用入口 + 创建的歌单 + 会员中心入口）+ 主内容区 + 页脚 + 底部全局播放器。

## 播放器架构（全局 audio + zustand）

- **唯一 `<audio>` 元素**：由 `src/components/GlobalAudio.jsx` 渲染并挂在 `App` 层（路由 `<Routes>` 之外），路由切换不卸载，因此**切换页面播放不中断**。组件把元素通过 `registerAudio()` 注册到 store 模块，并把 `play/pause/timeupdate/ended` 事件回灌给 store。
- **zustand store**（`src/store/playerStore.js`）持有全部播放状态与动作：
  - 状态：播放队列 `queue`、当前下标 `index`、`playing`、播放模式 `mode`（order 顺序 / random 随机 / loop 单曲循环）、`currentTime/duration`、`volume`、试听秒数 `trialSeconds`、队列面板开关、`fmMode`。
  - 动作：`playList(songs, i, fm)`、`playSong`、`togglePlay`、`prev/next`、`cycleMode`、`seek`（进度条可拖动）、`setVolume`、`toggleLike`、队列增删清空。
  - **取播放地址**：每次播放前调用 `GET /api/songs/{id}/url`；接口返回 30001（VIP 专享）/30002（付费单曲）/20003（已下架）等业务码时，直接以 toast 展示接口返回的提示文案；`trialSeconds > 0` 时到点暂停并提示登录（游客试听 60 秒）。
  - **FM 模式**：`next()` 改为调用 `/api/fm/next` 追加下一曲，播放器隐藏"上一首"。
  - **持久化**：队列、下标、模式、音量、进度节流写入 `localStorage`，刷新后恢复（点击播放时重新签发 URL 并 seek 回上次进度）。
- UI：`src/components/PlayerBar.jsx`（封面/歌名歌手/喜欢、上一首/播放/下一首/模式、可拖动进度条、音量、队列面板）。

## axios 封装与错误处理

`src/api/client.js`：

- 请求拦截器自动携带 `Authorization: Bearer <token>`；
- 响应拦截器解包统一响应体 `{code, message, data}`，`code=0/200` 视为成功直接返回 `data`；业务失败统一用手写 toast 组件提示（可对单个请求传 `silent: true` 关闭）；
- HTTP 401 时清除本地 token、提示并跳转登录页；
- 各页面/楼层接口互相独立，失败时展示占位/空态组件（`Loading` / `Empty`），不会白屏。

## 与后端联调说明

1. 启动后端（Spring Boot，端口 8080）后直接 `npm run dev` 即可，所有 `/api/**` 请求经 Vite 代理转发。
2. 认证：登录/注册成功后 `accessToken`、`refreshToken` 存 `localStorage`（key：`yinyu_token` / `yinyu_refresh_token`）；目前 token 过期走 401 → 重新登录（refresh rotation 接口预留未接）。
3. 播放依赖 MinIO 预签名 URL 与 HTTP Range（拖动 seek 由浏览器直接对 MinIO 发 Range 请求）。
4. 会员购买：演示走 `channel=MOCK`（需后端 `pay.mock=true`）；返回 `payUrl` 时按支付宝沙箱流程打开收银台并每 3 秒轮询 `GET /api/orders/{orderNo}` 确认到账。
5. 歌手头像：接口 `avatar` 为空或加载失败时，回退到 `public/static/img/singer/` 下的本地图片（按歌手 id 取模）。
6. 评论区：`src/components/CommentSection.jsx` 为通用组件（`targetType=SONG/PLAYLIST`），支持最热/最新分页、发表（未登录引导登录、40001 敏感词直接展示接口提示）、点赞/取消（响应 likeCount 回填）、一级评论+展开回复、删除本人评论；已接入歌单详情页与"正在播放"页。
7. 下载：歌曲行 hover 出现下载按钮，调 `GET /api/songs/{id}/download-url` 拿附件式预签名 URL 触发浏览器下载；权益/配额不足（30001/30002/30003）时 toast 接口提示文案。
