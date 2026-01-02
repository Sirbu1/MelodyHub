# 清除歌手缓存的 PowerShell 脚本

Write-Host "正在连接 Redis..." -ForegroundColor Yellow

try {
    # 使用 redis-cli 清除 artistCache
    $result = redis-cli KEYS "artistCache::*"
    
    if ($result) {
        Write-Host "找到 $($result.Count) 个歌手缓存键" -ForegroundColor Cyan
        
        # 删除所有匹配的键
        $deleteResult = redis-cli DEL $result
        
        if ($deleteResult) {
            Write-Host "成功清除 $deleteResult 个歌手缓存" -ForegroundColor Green
        } else {
            Write-Host "没有缓存需要清除" -ForegroundColor Yellow
        }
    } else {
        Write-Host "没有找到歌手缓存" -ForegroundColor Yellow
    }
    
    Write-Host "`n缓存清除完成！" -ForegroundColor Green
    Write-Host "请重启后端服务以使更改生效。" -ForegroundColor Cyan
    
} catch {
    Write-Host "错误: $_" -ForegroundColor Red
    Write-Host "请确保 Redis 服务正在运行，并且 redis-cli 在系统 PATH 中。" -ForegroundColor Yellow
}

Write-Host "`n按任意键退出..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

