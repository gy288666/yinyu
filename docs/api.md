# 音域 YINYU — 接口文档

- 版本：v1.0（与 requirements.md v1.0 对应）
- 日期：2026-07-26

---

## 1. 全局约定

### 1.1 基础

- Base Path：`/api`（下文路径均省略域名，如 `GET /api/songs`）。
- 编码：UTF-8；请求体 `application/json`（文件上传为 `multipart/form-data`）。
- 时间格式：`yyyy-MM-dd HH:mm:ss`（东八区）。
- 金额单位：元，字符串两位小数（如 `"15.00"`）。

### 1.2 统一响应体

```json
{
  "code": 0,
  "message": "success",
  "data": { }
}
```

- `code=0` 表示成功；非 0 见错误码表。`data` 可为对象、数组或 `null`。
- HTTP 状态：业务失败也返回 200 + 业务码；仅 401（未认证/token失效）、403（无权限）、404（路径不存在）、500（未捕获异常）使用对应 HTTP 状态码，body 仍为统一响应体。

### 1.3 分页

- 请求参数：`pageNum`（从 1 起，默认 1）、`pageSize`（默认 10，最大 100），一律放 query。
- 统一分页响应（下文用 `Page<T>` 指代）：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 87,
    "pages": 9,
    "list": [ ]
  }
}
```

### 1.4 认证

- 请求头：`Authorization: Bearer <accessToken>`。
- 门户用户与后台管理员为两套 token（payload 含 `userType: USER | ADMIN`），互不通用：用户 token 调 `/api/admin/**` 返回 403，反之亦然。
- accessToken 有效期 2h，refreshToken 7d；刷新采用 rotation（旧 refreshToken 一次性）。
- 权限档位标注约定（下文"权限"列）：
  - `公开`：无需 token；
  - `用户`：需门户用户登录；
  - `VIP`：需门户 VIP（服务端校验到期时间）；
  - `管理员:<标识>`：需后台登录且拥有该权限标识，如 `管理员:music:audit`。

### 1.5 错误码表

| code | 含义 | 说明 |
| --- | --- | --- |
| 0 | 成功 | |
| 10001 | 参数校验失败 | message 含具体字段错误 |
| 10002 | 用户名已存在 | 注册 |
| 10003 | 用户名或密码错误 | 登录 |
| 10004 | 账号已锁定 | 连续错 5 次锁 10 分钟 |
| 10005 | 账号已停用 | |
| 10006 | 验证码错误或过期 | 后台登录 |
| 10007 | token 无效或已过期 | 伴随 HTTP 401 |
| 10008 | refreshToken 已使用或无效 | |
| 20001 | 文件类型或大小不合法 | 上传 |
| 20002 | 资源不存在 | 通用 |
| 20003 | 歌曲已下架或不可用 | 播放地址 |
| 20004 | 资源状态不允许该操作 | 如重复审核 |
| 20005 | 存在引用不可删除 | 歌手/分类等 |
| 20006 | 无权访问该私密资源 | 私密歌单 |
| 30001 | VIP 专享，请开通会员 | 播放/下载拦截 |
| 30002 | 付费单曲，请先购买 | |
| 30003 | 今日下载配额已用完 | |
| 30004 | 订单不存在或已关闭 | |
| 30005 | 重复支付/回调已处理 | 幂等提示 |
| 40001 | 内容包含敏感词 | 评论/反馈 |
| 40301 | 无操作权限 | 伴随 HTTP 403 |
| 50000 | 系统繁忙 | 伴随 HTTP 500 |

---

## 2. 认证模块

### 2.1 用户注册 — `POST /api/auth/register`

权限：公开

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| username | string | 是 | 4-20 位字母数字 |
| password | string | 是 | 8-32 位，含字母与数字 |
| nickname | string | 是 | ≤20 字 |
| channel | string | 否 | 注册渠道 direct/search/share/activity，默认 direct |

响应：

```json
{
  "code": 0, "message": "success",
  "data": {
    "userId": 10001,
    "nickname": "夜航星",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 7200
  }
}
```

### 2.2 用户登录 — `POST /api/auth/login`

权限：公开。参数：`username`、`password`。响应同 2.1，另含 `vip`（bool）、`vipExpireAt`。失败返回 10003/10004/10005。

### 2.3 刷新 Token — `POST /api/auth/refresh`

权限：公开（凭 refreshToken）。参数：`refreshToken`。响应：新 token 对（同 2.1 data）。旧 refreshToken 立即作废，二次使用返回 10008。

### 2.4 用户登出 — `POST /api/auth/logout`

权限：用户。无参数。accessToken 入 Redis 黑名单。响应 `data: null`。

### 2.5 当前用户信息 — `GET /api/auth/me`

权限：用户

```json
{
  "code": 0, "message": "success",
  "data": {
    "userId": 10001, "username": "star01", "nickname": "夜航星",
    "avatar": "https://minio.example.com/image/avatar/10001.jpg",
    "gender": 1, "signature": "听歌写代码",
    "level": 3, "vip": true, "vipExpireAt": "2026-12-31 23:59:59",
    "likeCount": 42, "playlistCount": 5
  }
}
```

### 2.6 修改资料 — `PUT /api/auth/me`

权限：用户。参数：`nickname`、`avatar`、`gender`、`signature`（均可选）。

### 2.7 修改密码 — `PUT /api/auth/me/password`

权限：用户。参数：`oldPassword`、`newPassword`。成功后全部旧 token 失效。

### 2.8 上传头像 — `POST /api/auth/me/avatar`

权限：用户。`multipart/form-data`，字段 `file`（jpg/png ≤2MB）。响应 `data.url`。

### 2.9 管理员验证码 — `GET /api/admin/auth/captcha`

权限：公开。响应：`data: { captchaKey, imageBase64 }`，5 分钟有效。

### 2.10 管理员登录 — `POST /api/admin/auth/login`

权限：公开。参数：`username`、`password`、`captchaKey`、`captchaCode`。

```json
{
  "code": 0, "message": "success",
  "data": {
    "adminId": 1, "name": "超级管理员",
    "accessToken": "...", "refreshToken": "...", "expiresIn": 7200,
    "roles": ["SUPER_ADMIN"],
    "permissions": ["music:list", "music:add", "music:audit:pass", "system:role:edit"],
    "menus": [
      { "id": 1, "name": "音乐管理", "path": "/music", "icon": "customer-service", "children": [] }
    ]
  }
}
```

### 2.11 管理员刷新/登出 — `POST /api/admin/auth/refresh`、`POST /api/admin/auth/logout`

语义同 2.3/2.4，作用于管理员 token 体系。

### 2.12 管理员当前信息 — `GET /api/admin/auth/me`

权限：管理员。响应同 2.10 data（不含 token 字段），前端用于刷新后重建菜单与按钮权限。

---

## 3. 歌曲模块（门户）

### 3.1 歌曲分页 — `GET /api/songs`

权限：公开

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| pageNum/pageSize | int | 否 | 分页 |
| categoryId | long | 否 | 分类筛选 |
| singerId | long | 否 | 歌手筛选 |
| vip | bool | 否 | 仅 VIP 曲目（Hi-Res 专区用 `quality=lossless`） |
| quality | string | 否 | standard/lossless |
| sort | string | 否 | latest（默认）/hot |

响应 `Page<Song>`，Song 结构：

```json
{
  "id": 501, "name": "雨后钢琴", "duration": 245,
  "singerId": 21, "singerName": "青野",
  "albumId": 11, "albumName": "雨季练习曲",
  "cover": "https://minio.example.com/image/cover/501.jpg",
  "categoryId": 3, "categoryName": "钢琴",
  "vip": false, "price": "0.00", "quality": "standard",
  "playCount": 13204, "publishTime": "2026-06-01 10:00:00"
}
```

### 3.2 歌曲详情 — `GET /api/songs/{id}`

权限：公开。响应：Song + `lyric`（LRC 文本，纯音乐可为空）+ `liked`（登录时返回是否已喜欢）+ `purchased`（是否已购）。

### 3.3 获取播放地址（含埋点） — `GET /api/songs/{id}/url`

权限：公开（服务端按曲目权益判定，见下）

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | long | 是 | 路径参数 |

处理逻辑：校验曲目已上架（否则 20003）→ 权益校验（VIP 曲目非会员 30001；付费曲目未购 30002）→ 签发 MinIO 预签名 GET URL（默认 30 分钟，系统设置可调）→ Redis 埋点（30 秒去重后 `INCR song:play:total:{id}`、`ZINCRBY rank:day:{yyyyMMdd} 1 {id}`、写最近播放）。

```json
{
  "code": 0, "message": "success",
  "data": {
    "songId": 501,
    "url": "https://minio.example.com/music/501.mp3?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Expires=1800&X-Amz-Signature=...",
    "expiresIn": 1800,
    "trialSeconds": 0
  }
}
```

> 游客播放普通曲目时 `trialSeconds=60`，前端到点暂停并引导登录。音频拖动依赖 MinIO 原生 HTTP Range（206）。

### 3.4 新歌速递 — `GET /api/songs/newest`

权限：公开。参数：`limit`（默认 12，≤50）。响应 `Song[]`（近 30 天上架按时间倒序）。

### 3.5 歌曲歌词 — `GET /api/songs/{id}/lyric`

权限：公开。响应 `data: { songId, lyric }`。

---

## 4. 歌手模块（门户）

### 4.1 歌手分页 — `GET /api/singers`

权限：公开。参数：分页 + `area`（内地/欧美/日韩/其他）+ `type`（1男/2女/3组合/4厂牌）+ `initial`（A-Z）。响应 `Page<Singer>`：`{ id, name, avatar, area, type, songCount }`。

### 4.2 歌手详情 — `GET /api/singers/{id}`

权限：公开。响应：`{ id, name, avatar, area, type, intro, songCount, albumCount }`。

### 4.3 歌手歌曲 — `GET /api/singers/{id}/songs`

权限：公开。分页，响应 `Page<Song>`（仅已上架）。

### 4.4 歌手专辑 — `GET /api/singers/{id}/albums`

权限：公开。分页，响应 `Page<Album>`。

---

## 5. 专辑模块（门户）

### 5.1 专辑分页 — `GET /api/albums`

权限：公开。参数：分页 + `singerId`。响应 `Page<Album>`：`{ id, name, cover, singerId, singerName, publishDate, songCount }`。

### 5.2 专辑详情 — `GET /api/albums/{id}`

权限：公开。响应：Album + `intro` + `collected`（登录时）。

### 5.3 专辑曲目 — `GET /api/albums/{id}/songs`

权限：公开。响应 `Song[]`（按专辑内 trackNo 排序，不分页）。

---

## 6. 歌单模块（门户）

### 6.1 歌单广场 — `GET /api/playlists`

权限：公开。参数：分页 + `tag`（标签）+ `sort`（hot/latest）。响应 `Page<Playlist>`：`{ id, title, cover, tags, playCount, songCount, creatorName, official }`。

### 6.2 热门歌单 — `GET /api/playlists/hot`

权限：公开。参数：`limit`（默认 8）。取"推荐"标记 + 播放量排序。

### 6.3 歌单详情 — `GET /api/playlists/{id}`

权限：公开（私密歌单仅创建者，否则 20006）

```json
{
  "code": 0, "message": "success",
  "data": {
    "id": 301, "title": "深夜学习专注", "cover": "https://minio.example.com/image/pl/301.jpg",
    "tags": ["学习", "钢琴"], "intro": "适合深夜赶工",
    "official": true, "visibility": "PUBLIC",
    "creatorId": null, "creatorName": "音域运营",
    "playCount": 88214, "collectCount": 1023, "collected": false,
    "songs": [ { "id": 501, "name": "雨后钢琴", "singerName": "青野", "duration": 245, "vip": false } ]
  }
}
```

### 6.4 我的歌单列表 — `GET /api/my/playlists`

权限：用户。响应 `Playlist[]`（含"我喜欢的音乐"内置歌单，`builtin=true`）。

### 6.5 创建歌单 — `POST /api/my/playlists`

权限：用户。参数：`title`（必填）、`cover`、`intro`、`visibility`（PUBLIC/PRIVATE，默认 PUBLIC）。响应：新歌单对象。

### 6.6 修改歌单 — `PUT /api/my/playlists/{id}`

权限：用户（仅创建者）。参数同 6.5，均可选。内置歌单不可改名删除（20004）。

### 6.7 删除歌单 — `DELETE /api/my/playlists/{id}`

权限：用户（仅创建者）。

### 6.8 歌单添加/移除歌曲 — `POST /api/my/playlists/{id}/songs`、`DELETE /api/my/playlists/{id}/songs/{songId}`

权限：用户（仅创建者）。添加参数：`songId`；重复添加幂等。响应 `data: { songCount }`。

---

## 7. 分类模块

### 7.1 分类树 — `GET /api/categories`

权限：公开。响应两级树（仅启用项）：

```json
{ "code": 0, "message": "success", "data": [
  { "id": 1, "name": "场景", "children": [ { "id": 3, "name": "学习" }, { "id": 4, "name": "助眠" } ] },
  { "id": 2, "name": "风格", "children": [ { "id": 5, "name": "钢琴" }, { "id": 6, "name": "电子" } ] }
] }
```

---

## 8. 搜索模块

### 8.1 综合搜索 — `GET /api/search`

权限：公开

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| keyword | string | 是 | 非空，≤50 字 |
| type | string | 否 | song（默认）/singer/album |
| pageNum/pageSize | int | 否 | 分页 |

响应：`Page<Song|Singer|Album>`（按 type）。同时异步 `ZINCRBY search:hot 1 {keyword}`。

### 8.2 热搜词 — `GET /api/search/hot`

权限：公开。参数：`limit`（默认 10）。响应 `data: ["钢琴", "助眠", "白噪音"]`。

### 8.3 搜索联想 — `GET /api/search/suggest`

权限：公开。参数：`keyword`。响应前 10 条：`data: [ { "type": "song", "id": 501, "text": "雨后钢琴" }, { "type": "singer", "id": 21, "text": "青野" } ]`。

---

## 9. 排行榜模块

### 9.1 榜单列表 — `GET /api/ranks`

权限：公开。响应四个榜单概览（各带前 3 首）：`data: [ { "type": "HOT", "name": "热歌榜", "updateTime": "...", "top3": [Song] }, {"type": "NEW"...}, {"type": "ORIGINAL"...}, {"type": "SOAR"...} ]`。

### 9.2 榜单详情 — `GET /api/ranks/{type}`

权限：公开。`type`：HOT/NEW/ORIGINAL/SOAR。参数：`limit`（默认 50，≤100）。

```json
{ "code": 0, "message": "success", "data": {
  "type": "HOT", "name": "热歌榜", "updateTime": "2026-07-26 12:00:00",
  "songs": [ { "rankNo": 1, "trend": "UP", "trendDelta": 2, "song": { "id": 501, "name": "雨后钢琴", "playCount": 13204 } } ]
} }
```

---

## 10. 推荐 / 私人FM / 电台

### 10.1 每日推荐 — `GET /api/recommend/daily`

权限：用户（游客返回全站热门版）。响应 `data: { date: "2026-07-26", songs: [Song] }`（30 首，当日缓存）。

### 10.2 为你推荐 — `GET /api/recommend/songs`

权限：公开（登录后个性化）。参数：`limit`（默认 10）。响应 `Song[]`。

### 10.3 推荐歌单 — `GET /api/recommend/playlists`

权限：公开。参数：`limit`（默认 6）。响应 `Playlist[]`。

### 10.4 私人FM 下一曲 — `GET /api/fm/next`

权限：用户。响应单曲 Song + 已含播放地址逻辑提示（前端仍需调 3.3 换 URL）。7 天内不出现被标记"不喜欢"的曲目。

### 10.5 私人FM 不喜欢 — `POST /api/fm/dislike`

权限：用户。参数：`songId`。响应 `data: null`。

### 10.6 电台列表 — `GET /api/radios`

权限：公开。响应 `data: [ { "id": 7, "name": "助眠电台", "cover": "...", "desc": "白噪音与轻音乐" } ]`。

### 10.7 电台随机曲目 — `GET /api/radios/{id}/next`

权限：公开。响应单曲 Song（一轮内不重复，Redis 记录已播集合）。

---

## 11. 喜欢 / 收藏

### 11.1 喜欢歌曲 — `POST /api/likes/songs/{songId}`

权限：用户。幂等。响应 `data: { liked: true, likeCount: 43 }`。

### 11.2 取消喜欢 — `DELETE /api/likes/songs/{songId}`

权限：用户。响应 `data: { liked: false }`。

### 11.3 我喜欢的音乐 — `GET /api/likes/songs`

权限：用户。分页。响应 `Page<Song>`（按喜欢时间倒序）。

### 11.4 收藏/取消收藏歌单 — `POST /api/collections/playlists/{id}`、`DELETE /api/collections/playlists/{id}`

权限：用户。幂等。收藏专辑同形：`POST/DELETE /api/collections/albums/{id}`。

### 11.5 我的收藏 — `GET /api/collections`

权限：用户。参数：`type`（playlist/album）+ 分页。响应 `Page<Playlist|Album>`，失效项带 `invalid: true`。

---

## 12. 最近播放

### 12.1 最近播放列表 — `GET /api/recent`

权限：用户。分页（服务端最多保留 200 条，去重保序）。响应 `Page<{ song: Song, playAt: "2026-07-26 09:12:00" }>`。

### 12.2 清空最近播放 — `DELETE /api/recent`

权限：用户。响应 `data: null`。

> 写入：由 3.3 播放地址接口服务端自动记录，无独立上报接口。

---

## 13. 评论模块

### 13.1 评论分页 — `GET /api/comments`

权限：公开

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| targetType | string | 是 | SONG/PLAYLIST |
| targetId | long | 是 | 目标 ID |
| sort | string | 否 | hot（默认）/latest |
| pageNum/pageSize | int | 否 | 分页 |

```json
{ "code": 0, "message": "success", "data": {
  "pageNum": 1, "pageSize": 10, "total": 56, "pages": 6,
  "list": [ {
    "id": 9001, "userId": 10001, "nickname": "夜航星", "avatar": "...", "level": 3, "vip": true,
    "content": "适合下雨天循环", "likeCount": 12, "liked": false,
    "createTime": "2026-07-25 21:00:00",
    "replies": [ { "id": 9002, "nickname": "阿澈", "replyTo": "夜航星", "content": "+1", "createTime": "..." } ]
  } ]
} }
```

### 13.2 发表评论/回复 — `POST /api/comments`

权限：用户。参数：`targetType`、`targetId`、`content`（≤500 字）、`parentId`（回复时传）。敏感词返回 40001。

### 13.3 删除评论 — `DELETE /api/comments/{id}`

权限：用户（本人）或管理员（`管理员:comment:delete`，走 `/api/admin` 前缀等价接口）。父评论删除级联标记子回复。

### 13.4 评论点赞/取消 — `POST /api/comments/{id}/like`、`DELETE /api/comments/{id}/like`

权限：用户。幂等。响应 `data: { likeCount }`。

---

## 14. 下载模块

### 14.1 获取下载地址 — `GET /api/songs/{id}/download-url`

权限：用户（VIP 曲目需 VIP；付费曲目需已购）。校验每日配额（普通 10 / VIP 100，超限 30003）后签发附件式预签名 URL 并记下载记录。

```json
{ "code": 0, "message": "success", "data": {
  "url": "https://minio.example.com/music/501.mp3?response-content-disposition=attachment%3B%20filename%3D...&X-Amz-Signature=...",
  "expiresIn": 600, "quotaLeft": 9
} }
```

### 14.2 下载记录 — `GET /api/downloads`

权限：用户。分页。响应 `Page<{ song: Song, fileSize: 8123456, downloadAt: "..." }>`。

---

## 15. 会员与订单

### 15.1 会员套餐列表 — `GET /api/vip/plans`

权限：公开。响应：`data: [ { "id": 1, "name": "月卡", "days": 31, "price": "15.00", "originPrice": "20.00" }, { "id": 2, "name": "季卡", "days": 93, "price": "40.00" }, { "id": 3, "name": "年卡", "days": 366, "price": "128.00" } ]`。

### 15.2 创建订单 — `POST /api/orders`

权限：用户

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| orderType | string | 是 | VIP / SONG |
| planId | long | 条件 | orderType=VIP 时必填 |
| songId | long | 条件 | orderType=SONG 时必填 |

```json
{ "code": 0, "message": "success", "data": {
  "orderNo": "20260726103000123456", "orderType": "VIP",
  "subject": "音域VIP·月卡", "amount": "15.00",
  "status": "PENDING", "expireAt": "2026-07-26 10:45:00"
} }
```

> 同商品存在待支付订单时直接返回原订单（不重复创建）；已购单曲重复下单返回 20004。

### 15.3 发起支付 — `POST /api/orders/{orderNo}/pay`

权限：用户。参数：`channel`（ALIPAY/MOCK，MOCK 仅系统设置 `pay.mock=true` 时可用）。
响应（ALIPAY）：`data: { payUrl: "<支付宝沙箱收银台表单HTML或跳转URL>" }`；（MOCK）：直接完成支付，返回 `data: { status: "PAID" }`。

### 15.4 支付宝异步回调 — `POST /api/orders/alipay/notify`

权限：公开（支付宝服务端调用，验签）。`form` 参数为支付宝标准 notify 报文。验签失败仅记日志返回 `failure`；成功且订单待支付则置 PAID、发放权益（VIP 累加时长 / 单曲授权），幂等（重复 notify 返回 `success` 不重复发放）。响应体为纯文本 `success`/`failure`（支付宝要求，不走统一响应体）。

### 15.5 订单查询 — `GET /api/orders/{orderNo}`

权限：用户（仅本人）。响应：订单对象（含 `status: PENDING/PAID/CLOSED/REFUNDED`、`payTime`）。前端支付后轮询此接口确认到账。

### 15.6 我的订单 — `GET /api/orders`

权限：用户。参数：分页 + `status`。响应 `Page<Order>`。

### 15.7 我的已购单曲 — `GET /api/purchases/songs`

权限：用户。分页。响应 `Page<Song>`。

---

## 16. 运营内容（门户展示）

### 16.1 轮播图 — `GET /api/banners`

权限：公开。响应（仅启用且在有效期内，按 sort 升序）：
`data: [ { "id": 1, "image": "https://minio.example.com/image/banner/1.jpg", "title": "夏日歌单上新", "targetType": "PLAYLIST", "targetId": 301, "link": null } ]`。

### 16.2 公告列表/详情 — `GET /api/notices`、`GET /api/notices/{id}`

权限：公开。列表参数：`limit`（默认 5）。响应：`{ id, title, publishTime }`；详情另含 `content`（富文本）。

### 16.3 活动列表/详情 — `GET /api/activities`、`GET /api/activities/{id}`

权限：公开。列表分页，含 `status`（ONGOING/ENDED）；详情含富文本 `content`。

---

## 17. 用户反馈（门户）

### 17.1 提交反馈 — `POST /api/feedbacks`

权限：用户。参数：`type`（BUG/SUGGEST/COPYRIGHT/OTHER）、`content`（≤1000 字）、`images`（URL 数组 ≤3，先走通用上传）。

### 17.2 我的反馈 — `GET /api/feedbacks`

权限：用户。分页。响应含状态与后台回复：`{ id, type, content, status: "PENDING/REPLIED/CLOSED", reply, replyTime, createTime }`。

---

## 18. 后台管理接口（`/api/admin/**`）

> 通用说明：全部需管理员 token；"权限"列为权限标识（RBAC 按钮/接口粒度）。分页/响应体遵循全局约定。写操作自动记操作日志。

### 18.1 数据看板与统计

#### 18.1.1 看板汇总 — `GET /api/admin/dashboard/summary`（权限 `dashboard:view`）

```json
{ "code": 0, "message": "success", "data": {
  "songTotal": 1286, "userTotal": 5403, "todayPlayCount": 23011,
  "vipCount": 412, "revenue": "36520.00",
  "compare": { "songTotal": 0.02, "userTotal": 0.011, "todayPlayCount": -0.05, "vipCount": 0.03, "revenue": 0.08 }
} }
```

#### 18.1.2 播放量趋势 — `GET /api/admin/dashboard/play-trend`（权限 `dashboard:view`）

参数：`days`（7/30）。响应 `data: [ { "date": "2026-07-20", "count": 18023 } ]`（当日为 Redis 实时值）。

#### 18.1.3 用户来源渠道分布 — `GET /api/admin/dashboard/user-channels`（权限 `dashboard:view`）

响应 `data: [ { "channel": "direct", "count": 3200, "ratio": 0.59 }, { "channel": "share", "count": 1203, "ratio": 0.22 } ]`。

#### 18.1.4 实时动态 — `GET /api/admin/dashboard/events`（权限 `dashboard:view`）

参数：`limit`（默认 20）。响应 `data: [ { "type": "REGISTER|ORDER|AUDIT|COPYRIGHT_EXPIRE|TASK_FAIL", "text": "用户 star01 注册", "time": "..." } ]`。

#### 18.1.5 热门歌曲 TOP5 — `GET /api/admin/dashboard/hot-songs`（权限 `dashboard:view`）

响应 `data: [ { "songId": 501, "name": "雨后钢琴", "singerName": "青野", "todayPlayCount": 890 } ]`（今日 ZSet 前 5）。

#### 18.1.6 统计查询 — `GET /api/admin/stats`（权限 `stats:view`）

参数：`metric`（play/register/revenue）、`granularity`(day/week/month)、`startDate`、`endDate`（跨度 ≤1 年）。响应 `data: [ { "period": "2026-07", "value": 60233 } ]`。

#### 18.1.7 统计导出 — `GET /api/admin/stats/export`（权限 `stats:export`）

参数同 18.1.6。响应：CSV 文件流（`Content-Disposition: attachment`）。

### 18.2 音乐管理

#### 18.2.1 音乐分页 — `GET /api/admin/songs`（权限 `music:list`）

参数：分页 + `name`、`singerId`、`albumId`、`categoryId`、`status`（PENDING/ONLINE/REJECTED/OFFLINE）。响应 `Page<AdminSong>`（Song + `status`、`rejectReason`、`objectKey`、`fileSize`、`bitrate`、`createBy`）。

#### 18.2.2 上传音频文件 — `POST /api/admin/songs/upload`（权限 `music:add`）

`multipart/form-data` 字段 `file`（mp3/flac/wav ≤200MB）。后端解析时长/码率并暂存 MinIO。

```json
{ "code": 0, "message": "success", "data": {
  "objectKey": "tmp/8f2f0c.mp3", "duration": 245, "bitrate": 320, "fileSize": 9822011, "suggestName": "雨后钢琴"
} }
```

#### 18.2.3 新增音乐 — `POST /api/admin/songs`（权限 `music:add`）

参数：`name`、`singerId`、`albumId`、`categoryId`、`objectKey`（18.2.2 返回）、`cover`、`lyric`、`vip`、`price`、`quality`、`original`。创建后状态 PENDING。

#### 18.2.4 修改音乐 — `PUT /api/admin/songs/{id}`（权限 `music:edit`）

参数同 18.2.3，均可选。

#### 18.2.5 上/下架 — `PUT /api/admin/songs/{id}/status`（权限 `music:shelf`）

参数：`status`（ONLINE/OFFLINE）。仅已通过审核的曲目可操作（否则 20004）。

#### 18.2.6 删除音乐 — `DELETE /api/admin/songs/{id}`（权限 `music:delete`）

逻辑删除；MinIO 对象保留 30 天由清理任务处理。

#### 18.2.7 批量导入 — `POST /api/admin/songs/batch`（权限 `music:add`）

参数：`items: [ { objectKey, name, singerId, categoryId } ]`。响应逐条结果：`data: { successCount, failCount, failures: [ { name, reason } ] }`。

### 18.3 音乐审核

#### 18.3.1 审核分页 — `GET /api/admin/audits`（权限 `music:audit`）

参数：分页 + `status`（PENDING/PASSED/REJECTED）。响应 `Page<AdminSong>`（含试听用途，PENDING 曲目也可经 18.3.4 试听）。

#### 18.3.2 审核通过 — `PUT /api/admin/audits/{songId}/pass`（权限 `music:audit:pass`）

曲目置 ONLINE，记录审核人/时间。重复审核返回 20004。

#### 18.3.3 审核驳回 — `PUT /api/admin/audits/{songId}/reject`（权限 `music:audit:reject`）

参数：`reason`（≥5 字，必填）。支持批量：`PUT /api/admin/audits/batch`，参数 `songIds[]`、`action`（PASS/REJECT）、`reason`。响应 `data: { successCount, failCount }`。

#### 18.3.4 审核试听地址 — `GET /api/admin/audits/{songId}/url`（权限 `music:audit`）

同 3.3 签发预签名 URL，但不要求上架、不埋点。

### 18.4 内容资源 CRUD（歌手/专辑/歌单/分类/版权）

以下资源遵循同一 REST 模式，逐条列出（参数为对应实体字段，响应为实体或 `Page<实体>`）：

| # | 接口 | 方法+路径 | 权限 |
| --- | --- | --- | --- |
| 18.4.1 | 歌手分页 | `GET /api/admin/singers` | `singer:list` |
| 18.4.2 | 新增歌手 | `POST /api/admin/singers`（name/avatar/area/type/intro） | `singer:add` |
| 18.4.3 | 修改歌手 | `PUT /api/admin/singers/{id}` | `singer:edit` |
| 18.4.4 | 删除歌手 | `DELETE /api/admin/singers/{id}`（被引用返回 20005） | `singer:delete` |
| 18.4.5 | 上传歌手图片 | `POST /api/admin/singers/upload`（file，存 MinIO image 桶） | `singer:add` |
| 18.4.6 | 专辑分页 | `GET /api/admin/albums` | `album:list` |
| 18.4.7 | 新增专辑 | `POST /api/admin/albums`（name/cover/singerId/publishDate/intro） | `album:add` |
| 18.4.8 | 修改专辑 | `PUT /api/admin/albums/{id}` | `album:edit` |
| 18.4.9 | 删除专辑 | `DELETE /api/admin/albums/{id}` | `album:delete` |
| 18.4.10 | 专辑曲目排序 | `PUT /api/admin/albums/{id}/songs`（songIds 有序数组） | `album:edit` |
| 18.4.11 | 官方歌单分页 | `GET /api/admin/playlists` | `playlist:list` |
| 18.4.12 | 新增官方歌单 | `POST /api/admin/playlists`（title/cover/tags/intro/recommended/top） | `playlist:add` |
| 18.4.13 | 修改官方歌单 | `PUT /api/admin/playlists/{id}` | `playlist:edit` |
| 18.4.14 | 删除官方歌单 | `DELETE /api/admin/playlists/{id}` | `playlist:delete` |
| 18.4.15 | 歌单曲目维护 | `PUT /api/admin/playlists/{id}/songs`（songIds 有序数组） | `playlist:edit` |
| 18.4.16 | 分类树查询 | `GET /api/admin/categories`（含停用项） | `category:list` |
| 18.4.17 | 新增分类 | `POST /api/admin/categories`（name/parentId/sort/enabled） | `category:add` |
| 18.4.18 | 修改分类 | `PUT /api/admin/categories/{id}` | `category:edit` |
| 18.4.19 | 删除分类 | `DELETE /api/admin/categories/{id}`（被引用返回 20005） | `category:delete` |
| 18.4.20 | 版权分页 | `GET /api/admin/copyrights`（可按到期日筛选） | `copyright:list` |
| 18.4.21 | 新增版权 | `POST /api/admin/copyrights`（songId/owner/licenseType/startDate/endDate/fileUrl） | `copyright:add` |
| 18.4.22 | 修改版权 | `PUT /api/admin/copyrights/{id}` | `copyright:edit` |
| 18.4.23 | 删除版权 | `DELETE /api/admin/copyrights/{id}` | `copyright:delete` |

版权分页响应示例（其余 CRUD 响应从简）：

```json
{ "code": 0, "message": "success", "data": { "pageNum": 1, "pageSize": 10, "total": 3, "pages": 1,
  "list": [ { "id": 5, "songId": 501, "songName": "雨后钢琴", "owner": "青野工作室",
    "licenseType": "BUYOUT", "startDate": "2026-01-01", "endDate": "2027-01-01",
    "fileUrl": "https://minio.example.com/image/license/5.pdf", "expiringSoon": false } ] } }
```

### 18.5 用户运营

| # | 接口 | 方法+路径 | 权限 |
| --- | --- | --- | --- |
| 18.5.1 | 用户分页 | `GET /api/admin/users`（username/nickname/status/vip 筛选） | `user:list` |
| 18.5.2 | 用户详情 | `GET /api/admin/users/{id}`（资料+VIP到期+最近登录+等级） | `user:list` |
| 18.5.3 | 启用/停用用户 | `PUT /api/admin/users/{id}/status`（status: ENABLED/DISABLED，停用即拉黑 token） | `user:disable` |
| 18.5.4 | 重置用户密码 | `PUT /api/admin/users/{id}/password/reset`（返回随机密码，用户首登需改密） | `user:reset` |
| 18.5.5 | 会员分页 | `GET /api/admin/vips`（VIP 用户与到期时间） | `vip:list` |
| 18.5.6 | 调整 VIP 时长 | `PUT /api/admin/vips/{userId}`（deltaDays 可正可负，记日志） | `vip:adjust` |
| 18.5.7 | 等级规则查询 | `GET /api/admin/levels` | `level:list` |
| 18.5.8 | 等级规则修改 | `PUT /api/admin/levels`（[{level, minPlayMinutes}]，次日任务重算） | `level:edit` |
| 18.5.9 | 反馈分页 | `GET /api/admin/feedbacks`（type/status 筛选） | `feedback:list` |
| 18.5.10 | 回复/关闭反馈 | `PUT /api/admin/feedbacks/{id}`（reply 或 status=CLOSED） | `feedback:handle` |

18.5.1 响应示例：

```json
{ "code": 0, "message": "success", "data": { "pageNum": 1, "pageSize": 10, "total": 5403, "pages": 541,
  "list": [ { "id": 10001, "username": "star01", "nickname": "夜航星", "level": 3,
    "vip": true, "vipExpireAt": "2026-12-31 23:59:59", "status": "ENABLED",
    "channel": "share", "lastLoginAt": "2026-07-26 08:30:00", "createTime": "2026-05-01 12:00:00" } ] } }
```

### 18.6 运营内容管理

| # | 接口 | 方法+路径 | 权限 |
| --- | --- | --- | --- |
| 18.6.1 | 轮播图分页 | `GET /api/admin/banners` | `banner:list` |
| 18.6.2 | 新增轮播图 | `POST /api/admin/banners`（image/title/targetType/targetId/link/sort/startTime/endTime/enabled） | `banner:add` |
| 18.6.3 | 修改轮播图 | `PUT /api/admin/banners/{id}` | `banner:edit` |
| 18.6.4 | 删除轮播图 | `DELETE /api/admin/banners/{id}` | `banner:delete` |
| 18.6.5 | 公告分页 | `GET /api/admin/notices` | `notice:list` |
| 18.6.6 | 发布公告 | `POST /api/admin/notices`（title/content 富文本/publishTime） | `notice:add` |
| 18.6.7 | 修改公告 | `PUT /api/admin/notices/{id}` | `notice:edit` |
| 18.6.8 | 撤回/删除公告 | `DELETE /api/admin/notices/{id}` | `notice:delete` |
| 18.6.9 | 活动分页 | `GET /api/admin/activities` | `activity:list` |
| 18.6.10 | 新增活动 | `POST /api/admin/activities`（title/cover/content/startTime/endTime） | `activity:add` |
| 18.6.11 | 修改活动 | `PUT /api/admin/activities/{id}` | `activity:edit` |
| 18.6.12 | 活动上/下线 | `PUT /api/admin/activities/{id}/status`（ONLINE/OFFLINE） | `activity:shelf` |
| 18.6.13 | 删除活动 | `DELETE /api/admin/activities/{id}` | `activity:delete` |
| 18.6.14 | 通用图片上传 | `POST /api/admin/upload/image`（file，jpg/png ≤5MB，返回 url） | 任一后台写权限 |

### 18.7 订单管理（后台）

| # | 接口 | 方法+路径 | 权限 |
| --- | --- | --- | --- |
| 18.7.1 | 订单分页 | `GET /api/admin/orders`（orderNo/userId/status/时间范围筛选） | `order:list` |
| 18.7.2 | 标记退款 | `PUT /api/admin/orders/{orderNo}/refund`（reason，回收对应权益，记日志） | `order:refund` |
| 18.7.3 | 套餐管理 | `GET/POST/PUT /api/admin/vip-plans`（价格/时长配置） | `vip:plan` |

### 18.8 RBAC 管理

#### 18.8.1 管理员分页 — `GET /api/admin/admins`（权限 `system:admin:list`）

响应 `Page<{ id, username, name, roles: ["AUDITOR"], status, createTime }>`。

#### 18.8.2 新增管理员 — `POST /api/admin/admins`（权限 `system:admin:add`）

参数：`username`、`password`、`name`、`roleIds[]`。

#### 18.8.3 修改管理员/分配角色 — `PUT /api/admin/admins/{id}`（权限 `system:admin:edit`）

参数：`name`、`roleIds[]`、`status`。内置 admin 不可停用（20004）。

#### 18.8.4 重置管理员密码 — `PUT /api/admin/admins/{id}/password/reset`（权限 `system:admin:reset`）

#### 18.8.5 删除管理员 — `DELETE /api/admin/admins/{id}`（权限 `system:admin:delete`；admin 不可删）

#### 18.8.6 角色列表 — `GET /api/admin/roles`（权限 `system:role:list`）

响应 `data: [ { id, code: "AUDITOR", name: "审核员", remark, adminCount } ]`。

#### 18.8.7 新增/修改/删除角色 — `POST /api/admin/roles`、`PUT /api/admin/roles/{id}`、`DELETE /api/admin/roles/{id}`（权限 `system:role:add/edit/delete`）

参数：`code`、`name`、`remark`。被管理员引用的角色删除时返回 20005。

#### 18.8.8 角色授权 — `PUT /api/admin/roles/{id}/permissions`（权限 `system:role:grant`）

参数：`permissionIds[]`（权限树勾选结果）。保存后角色权限缓存版本 +1，成员刷新即生效。

#### 18.8.9 权限树查询 — `GET /api/admin/permissions`（权限 `system:perm:list`）

```json
{ "code": 0, "message": "success", "data": [
  { "id": 10, "type": "MENU", "name": "音乐管理", "path": "/music", "perm": "music:list", "children": [
    { "id": 11, "type": "BUTTON", "name": "新增", "perm": "music:add" },
    { "id": 12, "type": "BUTTON", "name": "审核通过", "perm": "music:audit:pass" }
  ] }
] }
```

#### 18.8.10 权限新增/修改/删除 — `POST /api/admin/permissions`、`PUT /api/admin/permissions/{id}`、`DELETE /api/admin/permissions/{id}`（权限 `system:perm:edit`）

参数：`type`（MENU/BUTTON）、`name`、`parentId`、`path`、`icon`、`perm`（唯一标识）、`sort`。

### 18.9 系统设置与操作日志

#### 18.9.1 设置查询 — `GET /api/admin/settings`（权限 `system:setting:view`）

响应 `data: { "site.name": "音域 YINYU", "trial.seconds": "60", "download.quota.normal": "10", "download.quota.vip": "100", "pay.mock": "true", "minio.presign.expire": "1800" }`。

#### 18.9.2 设置修改 — `PUT /api/admin/settings`（权限 `system:setting:edit`）

参数：键值对对象（仅传需修改项）。保存后刷新 Redis 配置缓存，无需重启生效。

#### 18.9.3 操作日志分页 — `GET /api/admin/logs`（权限 `system:log:list`）

参数：分页 + `adminName`、`module`、`startTime`、`endTime`。

```json
{ "code": 0, "message": "success", "data": { "pageNum": 1, "pageSize": 10, "total": 1204, "pages": 121,
  "list": [ { "id": 88, "adminName": "auditor01", "module": "音乐审核", "action": "驳回",
    "params": "{\"songId\":502,\"reason\":\"音质过低\"}", "ip": "10.0.0.8",
    "costMs": 45, "success": true, "createTime": "2026-07-26 09:00:00" } ] } }
```

---

## 附录 A：接口数量统计

| 模块 | 接口数 |
| --- | --- |
| 认证（用户 8 + 管理员 5） | 13 |
| 歌曲（含播放地址/歌词） | 5 |
| 歌手 | 4 |
| 专辑 | 3 |
| 歌单（广场 3 + 自建 6） | 9 |
| 分类 | 1 |
| 搜索 | 3 |
| 排行榜 | 2 |
| 推荐/私人FM/电台 | 7 |
| 喜欢/收藏 | 7 |
| 最近播放 | 2 |
| 评论 | 5 |
| 下载 | 2 |
| 会员与订单（含支付回调/已购） | 7 |
| 运营内容门户展示（轮播/公告/活动） | 5 |
| 反馈（门户） | 2 |
| 后台-看板与统计 | 7 |
| 后台-音乐管理 | 7 |
| 后台-音乐审核 | 5 |
| 后台-内容资源 CRUD（歌手/专辑/歌单/分类/版权） | 23 |
| 后台-用户运营 | 10 |
| 后台-运营内容管理 | 14 |
| 后台-订单管理 | 5 |
| 后台-RBAC | 15 |
| 后台-设置与日志 | 3 |
| **合计** | **166** |

## 附录 B：典型调用时序（最小链路）

1. 管理员：`POST /api/admin/auth/login` → `POST /api/admin/songs/upload` → `POST /api/admin/songs` →（审核员）`PUT /api/admin/audits/{id}/pass`
2. 用户：`POST /api/auth/login` → `GET /api/songs?sort=latest` → `GET /api/songs/{id}/url`（服务端埋点+记最近播放）→ 浏览器 Range 请求 MinIO 预签名 URL 播放
3. 购买 VIP：`GET /api/vip/plans` → `POST /api/orders` → `POST /api/orders/{orderNo}/pay` → 支付宝沙箱付款 → 支付宝调 `POST /api/orders/alipay/notify` → 前端轮询 `GET /api/orders/{orderNo}` 确认 PAID
