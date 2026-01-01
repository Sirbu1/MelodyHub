# ============================================
# MusicPlayer Stop All Script
# ============================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "    Melody Hub Player Stop Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Stop Java backend process
Write-Host "[1/3] Stopping Backend Service..." -ForegroundColor Yellow
$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue
if ($javaProcesses) {
    $javaProcesses | Stop-Process -Force
    Write-Host "  [OK] Java processes stopped" -ForegroundColor Green
} else {
    Write-Host "  [-] No running Java processes found" -ForegroundColor Gray
}

# Stop Node.js frontend processes (Vite dev server)
Write-Host "[2/3] Stopping Frontend Service..." -ForegroundColor Yellow
$nodeProcesses = Get-Process -Name "node" -ErrorAction SilentlyContinue
if ($nodeProcesses) {
    $nodeProcesses | Stop-Process -Force
    Write-Host "  [OK] Node.js processes stopped" -ForegroundColor Green
} else {
    Write-Host "  [-] No running Node.js processes found" -ForegroundColor Gray
}

# Stop MinIO
Write-Host "[3/3] Stopping MinIO..." -ForegroundColor Yellow
$minioProcess = Get-Process -Name "minio" -ErrorAction SilentlyContinue
if ($minioProcess) {
    $minioProcess | Stop-Process -Force
    Write-Host "  [OK] MinIO process stopped" -ForegroundColor Green
} else {
    Write-Host "  [-] No running MinIO process found" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "    All services stopped!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Note: MySQL and Redis services are not stopped (usually running as system services)" -ForegroundColor Yellow
Write-Host ""
