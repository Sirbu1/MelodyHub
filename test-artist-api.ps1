# 测试歌手 API 的 PowerShell 脚本

Write-Host "测试客户端歌手 API..." -ForegroundColor Cyan
Write-Host ""

# 测试参数
$apiUrl = "http://localhost:8080/artist/getAllArtists"
$body = @{
    pageNum = 1
    pageSize = 12
    artistName = $null
    gender = 3  # 原创歌手
    area = $null
} | ConvertTo-Json

Write-Host "请求 URL: $apiUrl" -ForegroundColor Yellow
Write-Host "请求参数:" -ForegroundColor Yellow
Write-Host $body -ForegroundColor Gray
Write-Host ""

try {
    $response = Invoke-RestMethod -Uri $apiUrl -Method Post -Body $body -ContentType "application/json"
    
    Write-Host "响应状态码: $($response.code)" -ForegroundColor $(if ($response.code -eq 0) { "Green" } else { "Red" })
    Write-Host "响应消息: $($response.message)" -ForegroundColor Cyan
    
    if ($response.data) {
        Write-Host "总记录数: $($response.data.total)" -ForegroundColor Green
        Write-Host "返回记录数: $($response.data.items.Count)" -ForegroundColor Green
        Write-Host ""
        
        if ($response.data.items.Count -gt 0) {
            Write-Host "歌手列表:" -ForegroundColor Cyan
            foreach ($artist in $response.data.items) {
                Write-Host "  - ID: $($artist.artistId), 名称: $($artist.artistName), 头像: $($artist.avatar)" -ForegroundColor White
            }
        } else {
            Write-Host "没有找到原创歌手" -ForegroundColor Yellow
        }
    } else {
        Write-Host "响应中没有 data 字段" -ForegroundColor Red
    }
    
} catch {
    Write-Host "错误: $_" -ForegroundColor Red
    Write-Host "请确保后端服务正在运行在 http://localhost:8080" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "按任意键退出..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

