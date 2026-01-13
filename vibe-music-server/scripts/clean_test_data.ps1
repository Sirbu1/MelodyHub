# ============================================
# 清理测试数据PowerShell脚本
# 功能：执行SQL脚本删除测试用户和测试歌曲
# 使用前请修改数据库连接信息
# ============================================

param(
    [string]$dbHost = "localhost",
    [string]$dbPort = "3306",
    [string]$dbName = "vibe_music",
    [string]$dbUser = "root",
    [string]$dbPassword = "3127",
    [switch]$Preview = $false  # 预览模式，只查看不删除
)

# 获取脚本所在目录
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$sqlFile = Join-Path $scriptDir "..\sql\clean_test_data_simple.sql"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "清理测试数据脚本" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "数据库: $dbName @ ${dbHost}:${dbPort}" -ForegroundColor Yellow
Write-Host "SQL文件: $sqlFile" -ForegroundColor Yellow
Write-Host ""

# 检查SQL文件是否存在
if (-not (Test-Path $sqlFile)) {
    Write-Host "错误: SQL文件不存在: $sqlFile" -ForegroundColor Red
    exit 1
}

# 构建MySQL连接字符串
$mysqlCmd = "mysql"
$mysqlArgs = @(
    "-h", $dbHost,
    "-P", $dbPort,
    "-u", $dbUser,
    "-p$dbPassword",
    $dbName
)

# 如果是预览模式，只执行查询语句
if ($Preview) {
    Write-Host "预览模式: 只查看测试数据，不执行删除" -ForegroundColor Green
    Write-Host ""
    
    $previewSql = @"
SET NAMES utf8mb4;
SELECT '测试用户数量' AS '类型', COUNT(*) AS '数量' 
FROM tb_user WHERE username LIKE 'testuser%' OR username LIKE 'test_user_%'
UNION ALL
SELECT '测试用户上传的歌曲', COUNT(*) 
FROM tb_song s 
INNER JOIN tb_user u ON s.creator_id = u.id 
WHERE u.username LIKE 'testuser%' OR u.username LIKE 'test_user_%'
UNION ALL
SELECT '测试用户收藏数', COUNT(*) 
FROM tb_user_favorite uf
INNER JOIN tb_user u ON uf.user_id = u.id 
WHERE u.username LIKE 'testuser%' OR u.username LIKE 'test_user_%';
"@
    
    $previewSql | & $mysqlCmd $mysqlArgs
    
    Write-Host ""
    Write-Host "如果要执行删除，请去掉 -Preview 参数" -ForegroundColor Yellow
    exit 0
}

# 确认操作
Write-Host "警告: 此操作将永久删除所有测试用户及其相关数据！" -ForegroundColor Red
Write-Host "包括:" -ForegroundColor Yellow
Write-Host "  - 所有 testuser* 和 test_user_* 用户" -ForegroundColor Yellow
Write-Host "  - 这些用户上传的歌曲" -ForegroundColor Yellow
Write-Host "  - 这些用户的收藏、评论、论坛帖子等" -ForegroundColor Yellow
Write-Host ""
$confirm = Read-Host "确认执行删除操作？(输入 'yes' 继续)"

if ($confirm -ne "yes") {
    Write-Host "操作已取消" -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "开始执行删除操作..." -ForegroundColor Green

# 执行SQL脚本
try {
    Get-Content $sqlFile -Encoding UTF8 | & $mysqlCmd $mysqlArgs
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "测试数据清理完成！" -ForegroundColor Green
        
        # 验证删除结果
        Write-Host ""
        Write-Host "验证删除结果:" -ForegroundColor Cyan
        $verifySql = @"
SET NAMES utf8mb4;
SELECT '剩余测试用户数' AS '类型', COUNT(*) AS '数量' 
FROM tb_user WHERE username LIKE 'testuser%' OR username LIKE 'test_user_%';
"@
        $verifySql | & $mysqlCmd $mysqlArgs
    } else {
        Write-Host ""
        Write-Host "错误: SQL执行失败，退出码: $LASTEXITCODE" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host ""
    Write-Host "错误: 执行SQL脚本时发生异常" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan

