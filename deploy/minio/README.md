# 音域（YinYu）MinIO 对象存储部署指南

MinIO 用于存放平台的四类文件，对应四个桶（bucket）：

| 桶名 | 用途 | 访问策略 |
| ---- | ---- | -------- |
| `music` | 歌曲音频文件 | 私有（通过后端签名/代理访问） |
| `cover` | 歌曲/歌单封面 | 匿名只读 |
| `avatar` | 用户头像 | 匿名只读 |
| `banner` | 首页轮播图 | 匿名只读 |

- **API 地址**：`http://localhost:9000`（Spring Boot 对接用这个端口）
- **Web 控制台**：`http://localhost:9001`
- **账号**：`minioadmin`（可用 `MINIO_ROOT_USER` 覆盖）；**密码**：经环境变量 `MINIO_ROOT_PASSWORD` 提供，不写入仓库

---

## 方式一：Docker Compose（推荐）

需要已安装 Docker（Windows 装 Docker Desktop）。先设置密码环境变量，再在本目录执行：

```bash
export MINIO_ROOT_PASSWORD=你的强密码   # Windows CMD 用 set，也可在本目录创建 .env 文件
docker compose up -d
```

compose 里包含一个 `minio-init` 初始化容器，会在 MinIO 健康检查通过后**自动创建四个桶**并把 `cover`/`avatar`/`banner` 设为匿名只读，执行完自动退出（容器状态显示 Exited (0) 是正常的）。

常用命令：

```bash
docker compose logs -f minio     # 查看日志
docker compose down              # 停止（数据保留在 yinyu-minio-data 卷中）
docker compose down -v           # 停止并删除数据（慎用）
```

## 方式二：本地二进制（不装 Docker）

### Windows

双击运行 `start-minio.bat`。脚本会：

1. 检测本目录是否有 `minio.exe`，没有则用 curl 自动下载（失败会提示手动下载地址：<https://dl.min.io/server/minio/release/windows-amd64/minio.exe>，下载后放到本目录即可）；
2. 用本目录下的 `data\` 作为数据目录启动，端口与账号和 Docker 方式完全一致。

关闭窗口即停止 MinIO。

### Linux / macOS

```bash
chmod +x start-minio.sh
./start-minio.sh
```

脚本会按系统/架构自动 wget/curl 下载 minio 二进制（Linux x86_64 为
`https://dl.min.io/server/minio/release/linux-amd64/minio`），然后以 `./data` 为数据目录前台启动。要后台运行可用：

```bash
nohup ./start-minio.sh > minio.log 2>&1 &
```

> 注意：二进制方式**不会自动建桶**，需按下一节手动创建。

---

## 建桶与匿名只读策略

### 控制台方式（推荐新手）

1. 浏览器打开 <http://localhost:9001>，用 `minioadmin` 与你设置的 `MINIO_ROOT_PASSWORD` 登录；
2. 左侧 **Buckets → Create Bucket**，依次创建 `music`、`cover`、`avatar`、`banner`；
3. 点进 `cover` 桶 → **Summary** 页的 **Access Policy** → 从 `Private` 改为 **`Public`**（即匿名可读）；`avatar`、`banner` 同样操作；
4. `music` 保持 `Private` 不动，音频通过后端预签名 URL 或流式接口访问。

> 控制台的 “Public” 实际下发的是允许匿名 `GetObject` 的只读策略；如需更精细控制可在 **Anonymous** 标签页里只添加 `readonly` 规则。

### mc 命令行方式

```bash
mc alias set yinyu http://localhost:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"
mc mb --ignore-existing yinyu/music yinyu/cover yinyu/avatar yinyu/banner
mc anonymous set download yinyu/cover
mc anonymous set download yinyu/avatar
mc anonymous set download yinyu/banner
mc anonymous get yinyu/cover      # 验证，应输出 download
```

设置成功后，前端可直接用 `http://<服务器IP>:9000/cover/xxx.jpg` 访问图片，无需签名。

---

## Spring Boot 对接配置样例

`pom.xml` 依赖：

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.17</version>
</dependency>
```

`application.yml`：

```yaml
minio:
  # 注意：局域网/生产环境要写机器 IP，不要写 localhost（见下方常见坑）
  endpoint: http://192.168.1.100:9000
  access-key: minioadmin
  secret-key: ${MINIO_ROOT_PASSWORD}   # 从环境变量读取，勿硬编码
  bucket:
    music: music
    cover: cover
    avatar: avatar
    banner: banner

spring:
  servlet:
    multipart:
      max-file-size: 100MB      # 单个文件上限（无损音频较大，默认 1MB 远远不够）
      max-request-size: 120MB   # 整个请求上限，要 >= max-file-size
```

对应的配置类与客户端 Bean：

```java
@Bean
public MinioClient minioClient(MinioProperties props) {
    return MinioClient.builder()
            .endpoint(props.getEndpoint())
            .credentials(props.getAccessKey(), props.getSecretKey())
            .build();
}
```

---

## 常见坑

1. **上传报错 `MaxUploadSizeExceededException` / 413**
   Spring Boot 默认 multipart 单文件上限只有 **1MB**，上传音频必然失败。务必调大
   `spring.servlet.multipart.max-file-size` 和 `max-request-size`（且后者不小于前者）。若前面还挂了 Nginx，也要同步调大 `client_max_body_size`。

2. **endpoint 要用 IP，别用 localhost**
   后端配置里写 `http://localhost:9000`，生成的文件 URL 也是 localhost，手机或其它机器上的浏览器访问会指向"它自己"，图片/音频全部加载失败。开发联调时就应写成本机局域网 IP（如 `http://192.168.1.100:9000`），生产环境写公网 IP 或域名。

3. **预签名 URL 与 endpoint 绑定**
   预签名 URL 的签名把请求的 Host 也算了进去，**用哪个 endpoint 生成，就必须用哪个地址访问**。用 `localhost:9000` 生成的预签名 URL，换成 IP 访问会报 `SignatureDoesNotMatch`。所以：
   - 生成预签名 URL 的 MinioClient，其 endpoint 必须是**前端实际访问**的那个地址；
   - 如果内网上传、外网下载走不同地址，需要配置两个 MinioClient（内网一个、对外一个），或在 MinIO 前挂反向代理并统一用代理域名做 endpoint。

4. **匿名读 404/403**
   桶策略没设置成匿名只读时，直接访问 `http://ip:9000/cover/xxx.jpg` 会返回 403（AccessDenied）。按上文用控制台设 Public 或 `mc anonymous set download` 即可。注意策略是桶级别的，新建的桶默认都是私有。

5. **9000 与 9001 别混用**
   SDK/endpoint 一律用 9000（S3 API）；9001 只是网页控制台，配到后端会报奇怪的 XML 解析错误。

6. **音频拖动进度条依赖 Range 请求**
   浏览器 `<audio>` 拖动进度需要服务端支持 HTTP Range（返回 206）。MinIO 原生支持；但如果你用后端接口代理音频流，记得透传 `Range` 请求头并返回 206，否则无法拖动进度。
