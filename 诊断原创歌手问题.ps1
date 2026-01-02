# 诊断原创歌手显示问题的完整脚本

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "原创歌手显示问题诊断工具" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. 检查后端服务
Write-Host "[1/5] 检查后端服务..." -ForegroundColor Yellow
$backend = netstat -ano | findstr ":8080" | findstr "LISTENING"
if ($backend) {
    Write-Host "  ✓ 后端服务正在运行" -ForegroundColor Green
} else {
    Write-Host "  ✗ 后端服务未运行！请先启动后端服务" -ForegroundColor Red
    exit 1
}

# 2. 检查数据库中的原创歌手
Write-Host "[2/5] 检查数据库中的原创歌手..." -ForegroundColor Yellow
try {
    $dbResult = mysql -u root -p3127 vibe_music -e "SELECT COUNT(*) as count FROM tb_artist WHERE gender = 3;" 2>&1
    $countLine = $dbResult | Select-String "\d+" | Select-Object -First 1
    if ($countLine) {
        Write-Host "  ✓ 数据库中有原创歌手记录" -ForegroundColor Green
    } else {
        Write-Host "  ! 数据库中没有原创歌手记录" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ✗ 无法连接数据库" -ForegroundColor Red
}

# 3. 检查符合条件的原创歌手
Write-Host "[3/5] 检查符合条件的原创歌手（有已通过审核的歌曲）..." -ForegroundColor Yellow
try {
    $validResult = mysql -u root -p3127 vibe_music -e "SELECT a.id, a.name FROM tb_artist a WHERE a.gender = 3 AND a.name IN (SELECT u.username FROM tb_user u INNER JOIN tb_song s ON u.id = s.creator_id WHERE s.is_original = true AND (s.audit_status = 1 OR s.audit_status IS NULL) GROUP BY u.username);" 2>&1
    $dataLines = $validResult | Select-String -Pattern "^\d+\s+"
    if ($dataLines) {
        Write-Host "  ✓ 找到符合条件的原创歌手：" -ForegroundColor Green
        $dataLines | ForEach-Object { Write-Host "    $_" -ForegroundColor White }
    } else {
        Write-Host "  ! 没有符合条件的原创歌手（可能没有已通过审核的原创歌曲）" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ✗ 查询失败：$($_.Exception.Message)" -ForegroundColor Red
}

# 4. 测试客户端 API
Write-Host "[4/5] 测试客户端歌手 API..." -ForegroundColor Yellow
try {
    $apiUrl = "http://localhost:8080/artist/getAllArtists"
    $body = @{
        pageNum = 1
        pageSize = 12
        artistName = $null
        gender = 3
        area = $null
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri $apiUrl -Method Post -Body $body -ContentType "application/json" -ErrorAction Stop
    
    if ($response.code -eq 0) {
        if ($response.data.total -gt 0) {
            Write-Host "  ✓ API 返回 $($response.data.total) 个原创歌手" -ForegroundColor Green
            Write-Host "    歌手列表：" -ForegroundColor Cyan
            foreach ($artist in $response.data.items) {
                Write-Host "      - ID: $($artist.artistId), 名称: $($artist.artistName)" -ForegroundColor White
            }
        } else {
            Write-Host "  ✗ API 返回 0 个原创歌手" -ForegroundColor Red
            Write-Host "    这说明查询逻辑有问题或缓存未清除" -ForegroundColor Yellow
        }
    } else {
        Write-Host "  ✗ API 返回错误：$($response.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "  ✗ API 请求失败：$($_.Exception.Message)" -ForegroundColor Red
}

# 5. 检查 Redis 缓存
Write-Host "[5/5] 检查 Redis 缓存..." -ForegroundColor Yellow
try {
    $cacheKeys = redis-cli KEYS "artistCache::*"
    if ($cacheKeys -and $cacheKeys.Count -gt 0) {
        Write-Host "  ! 发现 $($cacheKeys.Count) 个歌手缓存键" -ForegroundColor Yellow
        Write-Host "    建议清除缓存：redis-cli FLUSHDB" -ForegroundColor Cyan
    } else {
        Write-Host "  ✓ 没有歌手缓存" -ForegroundColor Green
    }
} catch {
    Write-Host "  ! 无法连接 Redis" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "诊断完成" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 建议
Write-Host "建议操作：" -ForegroundColor Yellow
Write-Host "1. 如果 API 返回 0 个原创歌手，请重启后端服务" -ForegroundColor White
Write-Host "2. 如果有缓存，请清除：redis-cli FLUSHDB" -ForegroundColor White
Write-Host "3. 查看后端日志，搜索 '客户端查询歌手列表'" -ForegroundColor White
Write-Host ""

