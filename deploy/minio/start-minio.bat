@echo off
rem =====================================================
rem  音域（YinYu）- Windows 免 Docker 启动 MinIO
rem  API:    http://localhost:9000
rem  控制台: http://localhost:9001
rem  账号:   minioadmin / 密码经环境变量 MINIO_ROOT_PASSWORD 提供
rem =====================================================
setlocal
chcp 65001 >nul

set "MINIO_EXE=%~dp0minio.exe"
set "DATA_DIR=%~dp0data"
set "MINIO_ROOT_USER=minioadmin"
if "%MINIO_ROOT_PASSWORD%"=="" (
    echo [错误] 请先设置环境变量 MINIO_ROOT_PASSWORD，例如：set MINIO_ROOT_PASSWORD=你的强密码
    pause
    exit /b 1
)

if not exist "%MINIO_EXE%" (
    echo [提示] 未找到 minio.exe，尝试自动下载...
    where curl >nul 2>nul
    if errorlevel 1 (
        echo [错误] 系统没有 curl，请手动下载:
        echo        https://dl.min.io/server/minio/release/windows-amd64/minio.exe
        echo        下载后放到本目录（%~dp0）再重新运行本脚本。
        pause
        exit /b 1
    )
    curl -L -o "%MINIO_EXE%" https://dl.min.io/server/minio/release/windows-amd64/minio.exe
    if errorlevel 1 (
        echo [错误] 下载失败，请检查网络，或手动下载:
        echo        https://dl.min.io/server/minio/release/windows-amd64/minio.exe
        pause
        exit /b 1
    )
)

if not exist "%DATA_DIR%" mkdir "%DATA_DIR%"

echo =====================================================
echo  MinIO 启动中...
echo  API:    http://localhost:9000
echo  控制台: http://localhost:9001
echo  账号:   %MINIO_ROOT_USER% / %MINIO_ROOT_PASSWORD%
echo  数据目录: %DATA_DIR%
echo  首次启动后请到控制台创建桶: music、cover、avatar、banner
echo  并将 cover/avatar/banner 的 Access Policy 设为 public（详见 README.md）
echo =====================================================

"%MINIO_EXE%" server "%DATA_DIR%" --address ":9000" --console-address ":9001"

pause
endlocal
