#!/usr/bin/env bash
# =====================================================
#  音域（YinYu）- Linux/macOS 免 Docker 启动 MinIO
#  API:    http://localhost:9000
#  控制台: http://localhost:9001
#  账号:   minioadmin / yinyu@minio123
# =====================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd)"
MINIO_BIN="$SCRIPT_DIR/minio"
DATA_DIR="$SCRIPT_DIR/data"

export MINIO_ROOT_USER="minioadmin"
export MINIO_ROOT_PASSWORD="yinyu@minio123"

# 根据系统选择下载地址
OS="$(uname -s)"
ARCH="$(uname -m)"
case "$OS" in
  Linux)
    case "$ARCH" in
      x86_64)  URL="https://dl.min.io/server/minio/release/linux-amd64/minio" ;;
      aarch64|arm64) URL="https://dl.min.io/server/minio/release/linux-arm64/minio" ;;
      *) echo "[错误] 不支持的架构: $ARCH"; exit 1 ;;
    esac
    ;;
  Darwin)
    case "$ARCH" in
      arm64)  URL="https://dl.min.io/server/minio/release/darwin-arm64/minio" ;;
      x86_64) URL="https://dl.min.io/server/minio/release/darwin-amd64/minio" ;;
      *) echo "[错误] 不支持的架构: $ARCH"; exit 1 ;;
    esac
    ;;
  *) echo "[错误] 不支持的系统: $OS"; exit 1 ;;
esac

if [ ! -x "$MINIO_BIN" ]; then
  echo "[提示] 未找到 minio 二进制，自动下载: $URL"
  if command -v wget >/dev/null 2>&1; then
    wget -O "$MINIO_BIN" "$URL"
  elif command -v curl >/dev/null 2>&1; then
    curl -L -o "$MINIO_BIN" "$URL"
  else
    echo "[错误] 系统没有 wget/curl，请手动下载 $URL 放到 $SCRIPT_DIR 下并 chmod +x"
    exit 1
  fi
  chmod +x "$MINIO_BIN"
fi

mkdir -p "$DATA_DIR"

echo "====================================================="
echo " MinIO 启动中..."
echo " API:    http://localhost:9000"
echo " 控制台: http://localhost:9001"
echo " 账号:   $MINIO_ROOT_USER / $MINIO_ROOT_PASSWORD"
echo " 数据目录: $DATA_DIR"
echo " 首次启动后请创建桶: music、cover、avatar、banner"
echo " 并将 cover/avatar/banner 设为匿名只读（详见 README.md）"
echo "====================================================="

exec "$MINIO_BIN" server "$DATA_DIR" --address ":9000" --console-address ":9001"
