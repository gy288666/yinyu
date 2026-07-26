# 音域 YINYU 部署启动指南（新手友好版）

本文提供 4 种启动方式，按你的情况选一种即可：

| 方式 | 适合谁 | 需要装什么 |
|---|---|---|
| [方式一：本地电脑直接跑](#方式一windows-本地电脑直接跑最详细) | 想在自己 Windows 电脑上开发调试 | JDK17、Node、MySQL8、Redis |
| [方式二：Docker 一键全套](#方式二docker-一键启动全套最省事) | 只想快速看到效果，不想装一堆软件 | 只装 Docker Desktop |
| [方式三：中间件用 Docker，代码本地跑](#方式三中间件用-docker应用本地跑推荐日常开发) | 日常开发推荐姿势 | Docker Desktop + JDK17 + Node |
| [方式四：Linux 云服务器部署](#方式四linux-云服务器部署简版) | 想发布到公网让别人访问 | 一台云服务器 |

先记住几个通用信息，后面所有方式都一样：

- **端口**：后端 `8080`，管理后台 `5173`，用户门户 `5174`，MySQL `3306`，Redis `6379`
- **数据库**：库名 `yinyu`，账号 `root`，密码 `gy288666`
- **演示账号**（密码都是 `123456`）：后台 `admin`（超管）、`auditor`（审核员）；门户 `demo_user`（VIP）、`music_fan`（普通用户）
- **访问入口**：门户 http://localhost:5174 ，管理后台 http://localhost:5173

---

## 方式一：Windows 本地电脑直接跑（最详细）

### 第 0 步：把代码下载到本地

装过 Git 的话：

```bash
git clone https://github.com/gy288666/yinyu.git
cd yinyu
```

没装 Git：到 GitHub 仓库页面点绿色 `Code` 按钮 → `Download ZIP`，解压到一个**路径里没有中文和空格**的目录（例如 `D:\yinyu`，不要放在"桌面\新建文件夹"这类中文路径下，容易出奇怪的问题）。

### 第 1 步：安装 JDK 17

1. 下载：搜索 "Adoptium Temurin 17" 或到 https://adoptium.net 下载 JDK 17 的 Windows `.msi` 安装包
2. 安装时**勾选 "Set JAVA_HOME variable"**（很重要，能省去手动配环境变量）
3. 验证：打开 CMD（按 `Win+R` 输入 `cmd` 回车），执行：

```bash
java -version
```

看到 `openjdk version "17.x.x"` 就成功了。如果提示"不是内部或外部命令"，重启 CMD 再试；还不行就要手动把 JDK 的 `bin` 目录加进系统环境变量 `Path`。

> 装 21 也可以（项目按 17 编译，21 向下兼容），但别装 8 或 11，会编译失败。

### 第 2 步：安装 Maven（编译后端用）

1. 下载：https://maven.apache.org/download.cgi 选 `apache-maven-3.9.x-bin.zip`
2. 解压到如 `D:\maven`，把 `D:\maven\bin` 加入系统环境变量 `Path`
   （右键"此电脑"→ 属性 → 高级系统设置 → 环境变量 → 系统变量里找到 Path → 编辑 → 新建 → 粘贴路径）
3. 验证：**新开一个** CMD 执行 `mvn -version`
4. （强烈建议）换阿里云镜像，不然国内下载依赖非常慢：编辑 `D:\maven\conf\settings.xml`，找到 `<mirrors>` 标签，在里面加：

```xml
<mirror>
  <id>aliyun</id>
  <mirrorOf>central</mirrorOf>
  <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

### 第 3 步：安装 Node.js（跑前端用）

1. 下载：https://nodejs.org 选 LTS 版本（20 或 22 都行）的 Windows 安装包，一路下一步
2. 验证：`node -v` 和 `npm -v` 都能输出版本号
3. （建议）换国内镜像加速：

```bash
npm config set registry https://registry.npmmirror.com
```

### 第 4 步：安装 MySQL 8

1. 下载：https://dev.mysql.com/downloads/installer/ 选 "MySQL Installer for Windows"（离线大包）
2. 安装时选 "Server only" 即可；配置阶段：
   - 端口保持默认 `3306`
   - **root 密码设置为 `gy288666`**（和项目配置保持一致，新手别自己改，改了要同步改后端配置）
   - 其余全部默认，一路 Next
3. 验证：CMD 执行（MySQL 的 bin 目录若不在 Path 里，用"MySQL 8.0 Command Line Client"快捷方式也行）：

```bash
mysql -uroot -pgy288666 -e "SELECT VERSION();"
```

4. **初始化数据库**（在仓库根目录执行，两条命令的顺序不能反）：

```bash
mysql -uroot -pgy288666 < sql/init.sql
mysql -uroot -pgy288666 < sql/test-data.sql
```

5. 验证建表成功：

```bash
mysql -uroot -pgy288666 -e "USE yinyu; SHOW TABLES;"
```

应该看到 37 张表。

> 用图形工具（Navicat/DBeaver/HeidiSQL）的同学：新建连接后打开 `sql/init.sql` 整个执行，再执行 `sql/test-data.sql`，效果一样。

### 第 5 步：安装 Redis

Windows 官方不发行 Redis，两个选择：

- **简单**：下载社区维护的 Windows 版 https://github.com/tporadowski/redis/releases 选 `Redis-x64-*.zip`，解压后双击 `redis-server.exe`，弹出的黑窗口**不要关**（关了 Redis 就停了）
- 或者用 [Memurai](https://www.memurai.com/)（Redis 的 Windows 兼容版，装完是后台服务）

验证：解压目录里双击 `redis-cli.exe`，输入 `ping`，返回 `PONG` 即可。

> Redis 必须启动，否则后端启动会连不上报错（验证码、播放计数、权限缓存都依赖它）。

### 第 6 步：MinIO（可选，新手先跳过）

**可以先不装。** 后端检测不到 MinIO 会自动降级：上传的文件存到 `backend/storage/` 本地目录，仓库自带的 165 首音乐通过 `resource/static/music/` 直接读取，功能完全不受影响。

以后想玩对象存储了，看 `deploy/minio/README.md`，有一键脚本。

### 第 7 步：启动后端

在仓库根目录，开一个 CMD：

```bash
cd backend
mvn spring-boot:run
```

第一次会下载很多依赖（换了阿里云镜像大约几分钟），最后看到类似：

```
Started YinyuApplication in 5.2 seconds
```

就是启动成功了。**这个窗口不要关。**

验证：浏览器打开 http://localhost:8080/api/categories ，看到 `{"code":0,...}` 开头的 JSON 就对了。

> 也可以先 `mvn package -DskipTests` 打包，再 `java -jar target\yinyu-backend.jar` 运行，效果一样。

### 第 8 步：启动两个前端

再开**两个新的** CMD 窗口：

窗口 A（管理后台）：

```bash
cd yinyu\web-admin
npm install
npm run dev
```

窗口 B（用户门户）：

```bash
cd yinyu\web-portal
npm install
npm run dev
```

`npm install` 只有第一次需要，以后直接 `npm run dev`。

### 第 9 步：开始使用

- 用户门户：http://localhost:5174 → 右上角登录 `demo_user / 123456` → 点任意歌曲播放，应该能听到声音、能拖进度条
- 管理后台：http://localhost:5173 → `admin / 123456` + 图形验证码 → 进入数据看板

到这里就全部跑通了。日常启动顺序：**先 MySQL 和 Redis（装成服务的开机自启动），再后端，再前端。**

---

## 方式二：Docker 一键启动全套（最省事）

只需要装一个 Docker Desktop，MySQL/Redis/后端/前端全部自动搞定。

### 第 1 步：安装 Docker Desktop

1. 下载：https://www.docker.com/products/docker-desktop/ （Windows 需要开启 WSL2，安装程序会引导，跟着提示做即可，中途可能要重启一次电脑）
2. 安装后启动 Docker Desktop，等左下角变成绿色的 "Engine running"
3. 验证：CMD 执行 `docker --version` 和 `docker compose version`

> 国内拉镜像慢的话，Docker Desktop → Settings → Docker Engine，在 JSON 里加：
> ```json
> "registry-mirrors": ["https://docker.m.daocloud.io", "https://dockerproxy.com"]
> ```

### 第 2 步：一键启动

在仓库根目录执行：

```bash
docker compose -f deploy/docker/docker-compose.yml up -d --build
```

第一次会构建镜像（编译后端 + 打包前端），大约 5~15 分钟，以后再启动只要几秒。

看状态：

```bash
docker compose -f deploy/docker/docker-compose.yml ps
```

5 个容器（mysql / redis / backend / web-admin / web-portal）都是 `running`（mysql 显示 healthy）即成功。

### 第 3 步：访问

- 门户 http://localhost:5174 ，后台 http://localhost:5173 （账号见文首）
- 数据库已**自动**建库建表并导入演示数据（首次启动时自动执行了 `sql/` 目录的脚本）
- 仓库自带的音乐已自动挂载进后端容器，点歌即可播放

### 常用命令

```bash
# 停止（数据保留在 docker 卷里，下次启动还在）
docker compose -f deploy/docker/docker-compose.yml down

# 看后端日志（排错必备）
docker logs -f yinyu-backend

# 改了代码后重新构建并启动
docker compose -f deploy/docker/docker-compose.yml up -d --build

# 彻底重置（删除数据卷，数据库回到初始演示数据）
docker compose -f deploy/docker/docker-compose.yml down -v
```

> 本机已经装过 MySQL 且 3306 被占用？编辑 `deploy/docker/docker-compose.yml`，把 mysql 的端口映射改成 `"3307:3306"` 即可（容器之间不受影响）。

---

## 方式三：中间件用 Docker，应用本地跑（推荐日常开发）

写代码时推荐这种：MySQL/Redis/MinIO 交给 Docker（不用自己装），后端前端在本地跑（改代码热更新快、方便断点调试）。

```bash
# 1. 启动中间件（MySQL 会自动初始化数据库）
docker compose -f deploy/docker/docker-compose-middleware.yml up -d

# 2. 本地启动后端（要求装了 JDK17 + Maven，见方式一第 1、2 步）
cd backend
mvn spring-boot:run

# 3. 本地启动前端（要求装了 Node，见方式一第 3 步）
cd web-admin && npm install && npm run dev
cd web-portal && npm install && npm run dev
```

后端配置里连的就是 `localhost:3306` / `localhost:6379`，与 Docker 映射出来的端口正好对上，**零配置修改**。

这套中间件里也带了 MinIO（http://localhost:9001 控制台，minioadmin / yinyu@minio123），想体验对象存储时登进去建 `music`、`cover`、`avatar`、`banner` 四个桶即可，不建也不影响使用（自动降级）。

---

## 方式四：Linux 云服务器部署（简版）

有一台云服务器（如 Ubuntu 22.04）想让别人也能访问时参考。**两条路线：**

### 路线 A：直接用 Docker（推荐）

```bash
# 1. 装 Docker
curl -fsSL https://get.docker.com | sh

# 2. 拉代码
git clone https://github.com/gy288666/yinyu.git && cd yinyu

# 3. 把音频地址前缀里的 localhost 换成服务器公网 IP（重要！否则别人电脑上播放不了音乐）
#    编辑 deploy/docker/docker-compose.yml，把
#    MINIO_STATICBASE: http://localhost:8080/static
#    改成
#    MINIO_STATICBASE: http://你的服务器IP:8080/static

# 4. 启动
docker compose -f deploy/docker/docker-compose.yml up -d --build
```

然后在云厂商控制台的**安全组**放行端口：`5173`、`5174`、`8080`。访问 `http://服务器IP:5174`。

### 路线 B：裸机部署（jar + Nginx）

思路（有 Linux 基础再选这条）：

1. `apt install openjdk-17-jre mysql-server redis-server nginx`
2. 导入 `sql/` 两个脚本；`backend` 目录 `mvn package` 得到 jar，用 systemd 托管：

```ini
# /etc/systemd/system/yinyu.service
[Unit]
Description=yinyu backend
After=mysql.service redis.service
[Service]
WorkingDirectory=/opt/yinyu/backend
ExecStart=/usr/bin/java -jar target/yinyu-backend.jar
Restart=always
[Install]
WantedBy=multi-user.target
```

3. 两个前端 `npm run build`，把各自 `dist/` 拷到 `/var/www/`，Nginx 配置参照 `deploy/docker/nginx-spa.conf`（把 `proxy_pass http://backend:8080` 改成 `http://127.0.0.1:8080`），配两个 server 块分别对应两个域名/端口
4. 后端 `application.yml` 的 `minio.static-base` 改成公网可访问的地址
5. 有域名的话用 `certbot` 一条命令上 HTTPS

### 上线前必改的安全项

- 所有演示账号密码（admin/auditor/demo_user/music_fan 的 `123456`）
- MySQL root 密码，且不要对公网开放 3306
- `application.yml` 里的 `jwt` 密钥换成随机长字符串
- Redis 设置密码或只监听 127.0.0.1

---

## 常见问题（FAQ）

**Q1：后端启动报 `Communications link failure` / `Connection refused`**
MySQL 没启动，或端口/密码不对。先执行 `mysql -uroot -pgy288666 -e "SELECT 1;"` 确认能连上；Docker 方式看 `docker ps` 里 mysql 是否 healthy。

**Q2：后端启动报 `Unable to connect to Redis`**
Redis 没启动。Windows 双击 `redis-server.exe` 的窗口是不是被关了？

**Q3：`mvn` 下载依赖特别慢或卡住**
没换阿里云镜像，回看方式一第 2 步第 4 点。

**Q4：`npm install` 报错或特别慢**
先 `npm config set registry https://registry.npmmirror.com` 再重试；报权限错就删掉项目里的 `node_modules` 文件夹重来。

**Q5：端口被占用（`Port 8080 was already in use`）**
找到占用者：`netstat -ano | findstr 8080`（Windows），结束那个进程；或者改后端端口（`backend/src/main/resources/application.yml` 的 `server.port`），注意两个前端 `vite.config.js` 里的代理地址要跟着改。

**Q6：页面能开，但接口全部报错/登录不上**
九成是后端没起来或起在别的端口。直接开 http://localhost:8080/api/categories 验证后端；再检查前端控制台（F12）报错里请求的是不是 8080。

**Q7：歌曲列表有，点播放没声音**
- 本地方式：确认仓库 `resource/static/music/` 里有 mp3 文件（Git 拉取时如果跳过了大文件会缺）
- Docker 方式：确认 compose 里挂载了 `../../resource/static:/app/static-extra:ro`
- 服务器部署：`MINIO_STATICBASE` 是不是还写着 localhost（见方式四第 3 步）

**Q8：管理后台登录说验证码错误**
验证码 5 分钟过期且一次性，点图片刷新一张再输；连续错 5 次账号锁 10 分钟。

**Q9：改了后端代码不生效**
`mvn spring-boot:run` 不会热更新，Ctrl+C 停掉重新跑；Docker 方式要 `up -d --build` 重新构建。

**Q10：Docker Desktop 启动失败 / WSL2 报错**
按报错提示执行 `wsl --update`；BIOS 里需开启虚拟化（Intel VT-x / AMD-V），任务管理器 → 性能 → CPU 能看到"虚拟化：已启用"。

---

## 附：各方式启动清单速查

| | MySQL | Redis | MinIO | 后端 | 前端 |
|---|---|---|---|---|---|
| 方式一 | 自己装并初始化 | 自己装 | 跳过（自动降级） | `mvn spring-boot:run` | `npm run dev` ×2 |
| 方式二 | 容器自动 | 容器自动 | 未包含（降级） | 容器自动 | 容器自动 |
| 方式三 | 容器自动 | 容器自动 | 容器可选 | 本地跑 | 本地跑 |
| 方式四 | 服务器装/容器 | 服务器装/容器 | 可选 | jar+systemd/容器 | Nginx 托管 dist |
