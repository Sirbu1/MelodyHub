# 简单清理Redis中的token缓存（使用FLUSHDB，会删除当前数据库的所有数据）
# 警告：此操作会删除Redis当前数据库的所有数据！

param(
    [string]$RedisHost = "localhost",
    [int]$RedisPort = 6379,
    [string]$RedisPassword = ""
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "清理Redis数据库工具" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Redis地址: ${RedisHost}:${RedisPort}" -ForegroundColor White
Write-Host ""
Write-Host "警告：此操作将删除Redis当前数据库的所有数据！" -ForegroundColor Red
Write-Host "包括：token、验证码、缓存等所有数据！" -ForegroundColor Red
Write-Host ""

# 询问是否继续
$confirmation = Read-Host "确定要继续吗？(Y/N)"
if ($confirmation -ne 'Y' -and $confirmation -ne 'y') {
    Write-Host "已取消操作" -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "正在清理Redis数据库..." -ForegroundColor Yellow

# 构建redis-cli命令
$redisCmd = "redis-cli"
if ($RedisPassword) {
    $redisCmd += " -a $RedisPassword"
}

# 执行FLUSHDB命令
$result = & $redisCmd FLUSHDB 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✓ Redis数据库清理成功！" -ForegroundColor Green
    Write-Host ""
    Write-Host "建议：" -ForegroundColor Cyan
    Write-Host "  1. 刷新前端页面" -ForegroundColor White
    Write-Host "  2. 所有用户需要重新登录" -ForegroundColor White
} else {
    Write-Host ""
    Write-Host "✗ 清理失败！" -ForegroundColor Red
    Write-Host $result -ForegroundColor Red
    exit 1
}

