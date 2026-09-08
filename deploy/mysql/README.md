# MySQL 8 本地开发环境搭建指南（Windows）

本项目使用 MySQL 8，root 密码通过环境变量 `MYSQL_ROOT_PASSWORD` 配置（Docker 部署由 `deploy/docker/.env` 提供，请勿写入仓库），字符集统一使用 `utf8mb4`。
以下提供 Docker 与安装包两种方式，任选其一。

## 方式一：Docker 启动（推荐）

安装 [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/) 后，在 PowerShell / CMD 中执行：

```bash
docker run -d --name yinyu-mysql ^
  -p 3306:3306 ^
  -e MYSQL_ROOT_PASSWORD=你的root密码 ^
  -e TZ=Asia/Shanghai ^
  -v yinyu-mysql-data:/var/lib/mysql ^
  mysql:8.0 ^
  --character-set-server=utf8mb4 ^
  --collation-server=utf8mb4_unicode_ci ^
  --default-time-zone=+08:00
```

> 说明：`^` 是 CMD 的续行符；PowerShell 下请改用反引号 `` ` ``，或写成一行。

常用操作：

```bash
docker start yinyu-mysql        # 启动
docker stop yinyu-mysql         # 停止
docker exec -it yinyu-mysql mysql -uroot -p   # 进入 mysql 客户端（回车后输入密码）
```

## 方式二：安装包方式

1. 到 https://dev.mysql.com/downloads/installer/ 下载 MySQL Installer（选择 8.0.x）。
2. 安装时选择 Server Only（或 Custom 勾选 MySQL Server 8.0）。
3. 配置向导中：
   - Authentication Method 可选默认的 `caching_sha2_password`，也可选 Legacy（`mysql_native_password`），两者本项目均兼容；
   - root 密码自定并妥善保存（应用侧通过 `MYSQL_ROOT_PASSWORD` 环境变量使用同一密码）。
4. 安装完成后确认 Windows 服务 `MySQL80` 已启动（`services.msc` 中查看，或命令行 `net start MySQL80`）。

### 字符集配置（utf8mb4）

编辑 MySQL 配置文件（Windows 安装包方式默认在
`C:\ProgramData\MySQL\MySQL Server 8.0\my.ini`；Linux 为 `/etc/mysql/my.cnf` 或
`/etc/mysql/mysql.conf.d/mysqld.cnf`），加入/修改以下内容后重启 MySQL 服务：

```ini
[mysqld]
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci
default-time-zone = '+08:00'

[client]
default-character-set = utf8mb4

[mysql]
default-character-set = utf8mb4
```

验证字符集：

```sql
SHOW VARIABLES LIKE 'character%';
SHOW VARIABLES LIKE 'collation%';
```

`character_set_server` 应为 `utf8mb4`。

## 初始化数据库（执行建表脚本）

在项目根目录下执行（Windows 需保证 `mysql` 命令在 PATH 中，或使用完整路径
`"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"`）：

```bash
# 建库建表
mysql -uroot -p --default-character-set=utf8mb4 < sql/init.sql

# 导入测试数据
mysql -uroot -p --default-character-set=utf8mb4 < sql/test-data.sql
```

Docker 方式则为：

```bash
docker exec -i yinyu-mysql mysql -uroot -p --default-character-set=utf8mb4 < sql/init.sql
docker exec -i yinyu-mysql mysql -uroot -p --default-character-set=utf8mb4 < sql/test-data.sql
```

验证：

```bash
mysql -uroot -p -e "SHOW DATABASES;"
```

## Spring Boot 数据源配置样例

`application.yml`（数据库名以 `sql/init.sql` 中实际创建的为准，下面以 `yinyu` 为例）：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/yinyu?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: ${MYSQL_ROOT_PASSWORD}   # 从环境变量读取，勿硬编码
```

说明：

- `useUnicode=true&characterEncoding=utf8mb4`：保证连接层使用 utf8mb4，中文与 emoji 不乱码；
- `serverTimezone=Asia/Shanghai`：避免 JDBC 与服务器时区不一致导致时间偏移；
- `allowPublicKeyRetrieval=true`：当 root 使用 `caching_sha2_password` 认证且未走 SSL 时需要；
- `useSSL=false`：本地开发关闭 SSL，生产环境按需开启。
