# 清理Redis中的token缓存
# 用于解决用户信息混淆问题

param(
    [string]$RedisHost = "localhost",
    [int]$RedisPort = 6379,
    [string]$RedisPassword = ""
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "清理Redis Token缓存工具" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Redis地址: ${RedisHost}:${RedisPort}" -ForegroundColor White
Write-Host ""

# 检查Redis是否可用
try {
    $redisTest = Test-NetConnection -ComputerName $RedisHost -Port $RedisPort -WarningAction SilentlyContinue
    if (-not $redisTest.TcpTestSucceeded) {
        Write-Host "错误：无法连接到Redis服务器 ${RedisHost}:${RedisPort}" -ForegroundColor Red
        Write-Host "提示：请确保Redis服务正在运行" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "错误：无法连接到Redis服务器 ${RedisHost}:${RedisPort}" -ForegroundColor Red
    Write-Host "提示：请确保Redis服务正在运行" -ForegroundColor Yellow
    exit 1
}

Write-Host "正在连接Redis..." -ForegroundColor Yellow

# 检查redis-cli是否可用
try {
    $null = Get-Command redis-cli -ErrorAction Stop
} catch {
    Write-Host "错误：找不到 redis-cli 命令" -ForegroundColor Red
    Write-Host "提示：请将Redis bin目录添加到PATH，或使用完整路径" -ForegroundColor Yellow
    Write-Host "例如: redis-cli.exe 的完整路径" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "警告：此操作将删除Redis中所有的token缓存！" -ForegroundColor Yellow
Write-Host "这会导致所有已登录用户需要重新登录。" -ForegroundColor Yellow
Write-Host ""

# 询问是否继续
$confirmation = Read-Host "是否继续清理？(Y/N)"
if ($confirmation -ne 'Y' -and $confirmation -ne 'y') {
    Write-Host "已取消操作" -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "正在清理Redis token缓存..." -ForegroundColor Yellow

# 构建redis-cli命令
$redisCmd = "redis-cli"
if ($RedisPassword) {
    $redisCmd += " -a $RedisPassword"
}

# 方法1：使用KEYS命令查找所有token（注意：在生产环境可能影响性能）
Write-Host "正在查找所有token..." -ForegroundColor Cyan
$keys = & $redisCmd KEYS "*" 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "错误：无法执行Redis命令" -ForegroundColor Red
    Write-Host $keys -ForegroundColor Red
    exit 1
}

# 过滤出token相关的key
$tokenKeys = $keys | Where-Object { 
    $_ -match "^user:token:" -or 
    ($_ -notmatch "^verificationCode:" -and $_ -notmatch "^songCache:" -and $_ -notmatch "^userCache:" -and $_ -notmatch "^artistCache:" -and $_ -notmatch "^playlistCache:")
}

$tokenCount = ($tokenKeys | Measure-Object).Count
Write-Host "找到 $tokenCount 个token相关的key" -ForegroundColor White

if ($tokenCount -eq 0) {
    Write-Host "没有找到需要清理的token" -ForegroundColor Green
    exit 0
}

# 询问是否删除
$deleteConfirmation = Read-Host "确定要删除这 $tokenCount 个token吗？(Y/N)"
if ($deleteConfirmation -ne 'Y' -and $deleteConfirmation -ne 'y') {
    Write-Host "已取消删除" -ForegroundColor Yellow
    exit 0
}

# 删除token
$deletedCount = 0
foreach ($key in $tokenKeys) {
    $key = $key.Trim()
    if ($key) {
        $result = & $redisCmd DEL $key 2>&1
        if ($LASTEXITCODE -eq 0) {
            $deletedCount++
        }
    }
}

Write-Host ""
Write-Host "✓ 清理完成！" -ForegroundColor Green
Write-Host "已删除 $deletedCount 个token" -ForegroundColor White
Write-Host ""
Write-Host "建议：" -ForegroundColor Cyan
Write-Host "  1. 刷新前端页面" -ForegroundColor White
Write-Host "  2. 所有用户需要重新登录" -ForegroundColor White

