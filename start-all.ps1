# ============================================
# MusicPlayer Start All Script
# ============================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "    Vibe Music Player Start Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Project root directory
$ProjectRoot = $PSScriptRoot
$ServerDir = Join-Path $ProjectRoot "vibe-music-server"
$ClientDir = Join-Path $ProjectRoot "vibe-music-client"
$AdminDir = Join-Path $ProjectRoot "vibe-music-admin"
$MinioExe = "D:\MinIO\minio.exe"
$MinioDataDir = "D:\MinIO\data"

# ============================================
# 1. Start MySQL
# ============================================
Write-Host "[1/6] Starting MySQL..." -ForegroundColor Yellow
try {
    $mysqlService = Get-Service -Name "MySQL*" -ErrorAction SilentlyContinue
    if ($mysqlService) {
        if ($mysqlService.Status -ne "Running") {
            Start-Service -Name $mysqlService.Name
            Write-Host "  [OK] MySQL service started" -ForegroundColor Green
        } else {
            Write-Host "  [OK] MySQL service already running" -ForegroundColor Green
        }
        
        # Wait for MySQL to be ready with retry mechanism
        Write-Host "  Waiting for MySQL to be ready..." -ForegroundColor Cyan
        $maxAttempts = 30
        $attempt = 0
        $mysqlReady = $false
        
        while ($attempt -lt $maxAttempts -and -not $mysqlReady) {
            $attempt++
            try {
                # Test MySQL connection
                $null = mysql -u root -p3127 -e "SELECT 1;" 2>&1 | Out-Null
                if ($LASTEXITCODE -eq 0) {
                    $mysqlReady = $true
                    Write-Host "  [OK] MySQL connection successful" -ForegroundColor Green
                } else {
                    Start-Sleep -Seconds 2
                    if ($attempt % 5 -eq 0) {
                        Write-Host "  Waiting for MySQL... ($attempt/$maxAttempts)" -ForegroundColor Gray
                    }
                }
            } catch {
                Start-Sleep -Seconds 2
                if ($attempt % 5 -eq 0) {
                    Write-Host "  Waiting for MySQL... ($attempt/$maxAttempts)" -ForegroundColor Gray
                }
            }
        }
        
        if (-not $mysqlReady) {
            Write-Host "  [!] MySQL connection test failed after $maxAttempts attempts" -ForegroundColor Red
        }
    } else {
        Write-Host "  [!] MySQL service not found, please start manually" -ForegroundColor Red
    }
} catch {
    Write-Host "  [!] MySQL start failed" -ForegroundColor Red
}

# ============================================
# 2. Start Redis
# ============================================
Write-Host "[2/6] Starting Redis..." -ForegroundColor Yellow
try {
    $redisService = Get-Service -Name "Redis*" -ErrorAction SilentlyContinue
    if ($redisService) {
        if ($redisService.Status -ne "Running") {
            Start-Service -Name $redisService.Name
            Write-Host "  [OK] Redis service started" -ForegroundColor Green
        } else {
            Write-Host "  [OK] Redis service already running" -ForegroundColor Green
        }
    } else {
        $redisPath = Get-Command redis-server -ErrorAction SilentlyContinue
        if ($redisPath) {
            Start-Process -FilePath "redis-server" -WindowStyle Minimized
            Write-Host "  [OK] Redis started via command" -ForegroundColor Green
            Start-Sleep -Seconds 2
        } else {
            Write-Host "  [!] Redis not found, please start manually" -ForegroundColor Red
        }
    }
    
    # Wait for Redis to be ready by checking if port 6379 is listening
    Write-Host "  Waiting for Redis to be ready..." -ForegroundColor Cyan
    $maxAttempts = 30
    $attempt = 0
    $redisReady = $false
    
    while ($attempt -lt $maxAttempts -and -not $redisReady) {
        $attempt++
        try {
            $connection = Test-NetConnection -ComputerName localhost -Port 6379 -WarningAction SilentlyContinue -InformationLevel Quiet -ErrorAction SilentlyContinue
            if ($connection) {
                $redisReady = $true
                Write-Host "  [OK] Redis is ready!" -ForegroundColor Green
            } else {
                Start-Sleep -Seconds 1
                if ($attempt % 5 -eq 0) {
                    Write-Host "  Waiting for Redis... ($attempt/$maxAttempts)" -ForegroundColor Gray
                }
            }
        } catch {
            Start-Sleep -Seconds 1
            if ($attempt % 5 -eq 0) {
                Write-Host "  Waiting for Redis... ($attempt/$maxAttempts)" -ForegroundColor Gray
            }
        }
    }
    
    if (-not $redisReady) {
        Write-Host "  [!] Redis may not be ready yet" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  [!] Redis start failed" -ForegroundColor Red
}

# ============================================
# 3. Start MinIO
# ============================================
Write-Host "[3/6] Starting MinIO..." -ForegroundColor Yellow
try {
    if (Test-Path $MinioExe) {
        $minioProcess = Get-Process -Name "minio" -ErrorAction SilentlyContinue
        if (-not $minioProcess) {
            if (-not (Test-Path $MinioDataDir)) {
                New-Item -ItemType Directory -Path $MinioDataDir | Out-Null
            }
            Start-Process -FilePath $MinioExe -ArgumentList "server",$MinioDataDir,"--console-address",":9001" -WindowStyle Minimized
            Write-Host "  [OK] MinIO started (API: http://127.0.0.1:9000)" -ForegroundColor Green
        } else {
            Write-Host "  [OK] MinIO already running" -ForegroundColor Green
        }
        
        # Wait for MinIO to be ready by checking if port 9000 is listening
        Write-Host "  Waiting for MinIO to be ready..." -ForegroundColor Cyan
        $maxAttempts = 30
        $attempt = 0
        $minioReady = $false
        
        while ($attempt -lt $maxAttempts -and -not $minioReady) {
            $attempt++
            try {
                $connection = Test-NetConnection -ComputerName 127.0.0.1 -Port 9000 -WarningAction SilentlyContinue -InformationLevel Quiet -ErrorAction SilentlyContinue
                if ($connection) {
                    # Give MinIO a bit more time to fully initialize after port opens
                    Start-Sleep -Seconds 2
                    $minioReady = $true
                    Write-Host "  [OK] MinIO is ready!" -ForegroundColor Green
                } else {
                    Start-Sleep -Seconds 1
                    if ($attempt % 5 -eq 0) {
                        Write-Host "  Waiting for MinIO... ($attempt/$maxAttempts)" -ForegroundColor Gray
                    }
                }
            } catch {
                Start-Sleep -Seconds 1
                if ($attempt % 5 -eq 0) {
                    Write-Host "  Waiting for MinIO... ($attempt/$maxAttempts)" -ForegroundColor Gray
                }
            }
        }
        
        if (-not $minioReady) {
            Write-Host "  [!] MinIO may not be ready yet" -ForegroundColor Yellow
        } else {
            # Remind user to ensure bucket exists
            Write-Host "  [INFO] Please ensure 'vibe-music-data' bucket exists in MinIO" -ForegroundColor Cyan
            Write-Host "         Console: http://127.0.0.1:9001 (username: minioadmin, password: minioadmin)" -ForegroundColor Gray
        }
    } else {
        Write-Host "  [!] MinIO not found at $MinioExe" -ForegroundColor Red
    }
} catch {
    Write-Host "  [!] MinIO start failed" -ForegroundColor Red
}

# ============================================
# 4. Build and Start Backend (wait for completion)
# ============================================
Write-Host "[4/6] Building and Starting Backend..." -ForegroundColor Yellow

# Check if port 8080 is already in use
Write-Host "  Checking if port 8080 is available..." -ForegroundColor Cyan
$port8080InUse = $false
try {
    $connection = Test-NetConnection -ComputerName localhost -Port 8080 -WarningAction SilentlyContinue -InformationLevel Quiet -ErrorAction SilentlyContinue
    if ($connection) {
        $port8080InUse = $true
        Write-Host "  [!] Port 8080 is already in use" -ForegroundColor Yellow
        
        # Try to find and kill the process using port 8080
        Write-Host "  Attempting to free port 8080..." -ForegroundColor Cyan
        try {
            # Find process using port 8080 using PowerShell native cmdlet
            $tcpConnection = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
            if ($tcpConnection) {
                $pid = $tcpConnection.OwningProcess
                if ($pid) {
                    $process = Get-Process -Id $pid -ErrorAction SilentlyContinue
                    if ($process) {
                        Write-Host "  Found process: $($process.ProcessName) (PID: $pid)" -ForegroundColor Gray
                        # Check if it's a Java process (likely our backend)
                        if ($process.ProcessName -eq "java" -or $process.Path -like "*java*" -or $process.Path -like "*vibe-music-server*") {
                            Write-Host "  Stopping Java process (likely previous backend instance)..." -ForegroundColor Yellow
                            Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
                            Start-Sleep -Seconds 3
                            
                            # Verify port is now free
                            $connectionAfter = Test-NetConnection -ComputerName localhost -Port 8080 -WarningAction SilentlyContinue -InformationLevel Quiet -ErrorAction SilentlyContinue
                            if (-not $connectionAfter) {
                                Write-Host "  [OK] Port 8080 is now free" -ForegroundColor Green
                                $port8080InUse = $false
                            } else {
                                Write-Host "  [!] Port 8080 is still in use after stopping process" -ForegroundColor Yellow
                            }
                        } else {
                            Write-Host "  [!] Port 8080 is used by non-Java process: $($process.ProcessName)" -ForegroundColor Yellow
                            Write-Host "  Please manually stop the process or use a different port" -ForegroundColor Yellow
                        }
                    }
                }
            } else {
                Write-Host "  [!] Could not identify process using port 8080" -ForegroundColor Yellow
            }
        } catch {
            Write-Host "  [!] Error while trying to free port 8080: $($_.Exception.Message)" -ForegroundColor Yellow
        }
        
        if ($port8080InUse) {
            Write-Host "  [!] Warning: Port 8080 is still in use. Backend may fail to start." -ForegroundColor Red
            Write-Host "  Please manually close the application using port 8080 and try again." -ForegroundColor Yellow
        }
    } else {
        Write-Host "  [OK] Port 8080 is available" -ForegroundColor Green
    }
} catch {
    Write-Host "  [!] Could not check port 8080 status" -ForegroundColor Yellow
}

Write-Host "  Building backend project, please wait..." -ForegroundColor Cyan

Set-Location $ServerDir
mvn clean package -DskipTests

if ($LASTEXITCODE -eq 0) {
    Write-Host "  [OK] Backend build success" -ForegroundColor Green
    Write-Host "  Starting backend server in new window..." -ForegroundColor Cyan
    Start-Process powershell -ArgumentList "-NoExit","-Command","cd `"$ServerDir`"; java -jar target/vibe-music-server-0.0.1-SNAPSHOT.jar"
    Write-Host "  [OK] Backend server started" -ForegroundColor Green
    
    # Wait for backend to be ready by checking if port 8080 is listening AND HTTP response
    Write-Host "  Waiting for backend to be ready..." -ForegroundColor Yellow
    $maxAttempts = 60  # Increase to 60 seconds for backend initialization
    $attempt = 0
    $backendReady = $false
    
    while ($attempt -lt $maxAttempts -and -not $backendReady) {
        $attempt++
        try {
            # First check if port 8080 is listening
            $connection = Test-NetConnection -ComputerName localhost -Port 8080 -WarningAction SilentlyContinue -InformationLevel Quiet -ErrorAction SilentlyContinue
            if ($connection) {
                # Port is open, try to make an HTTP request to verify backend is fully ready
                try {
                    $response = Invoke-WebRequest -Uri "http://localhost:8080" -Method Get -TimeoutSec 3 -ErrorAction SilentlyContinue -UseBasicParsing
                    # If we get any HTTP response (even 404), backend is ready
                    $backendReady = $true
                    Write-Host "  [OK] Backend is ready!" -ForegroundColor Green
                } catch {
                    # HTTP request failed, backend might still be starting
                    Start-Sleep -Seconds 2
                    if ($attempt % 10 -eq 0) {
                        Write-Host "  Waiting for backend HTTP response... ($attempt/$maxAttempts)" -ForegroundColor Gray
                    }
                }
            } else {
                Start-Sleep -Seconds 2
                if ($attempt % 10 -eq 0) {
                    Write-Host "  Waiting for backend port 8080... ($attempt/$maxAttempts)" -ForegroundColor Gray
                }
            }
        } catch {
            # Backend not ready yet, continue waiting
            Start-Sleep -Seconds 2
            if ($attempt % 10 -eq 0) {
                Write-Host "  Waiting... ($attempt/$maxAttempts)" -ForegroundColor Gray
            }
        }
    }
    
    if (-not $backendReady) {
        Write-Host "  [!] Backend may not be fully ready yet" -ForegroundColor Yellow
        Write-Host "  [!] Please check the backend window for error messages" -ForegroundColor Yellow
        Write-Host "  [!] Common issues:" -ForegroundColor Yellow
        Write-Host "      - Database connection failed (check MySQL)" -ForegroundColor Gray
        Write-Host "      - Redis connection failed (check Redis)" -ForegroundColor Gray
        Write-Host "      - MinIO connection failed or bucket not found (check MinIO)" -ForegroundColor Gray
    }
} else {
    Write-Host "  [!] Backend build failed!" -ForegroundColor Red
    Set-Location $ProjectRoot
    Read-Host "Press Enter to exit"
    exit 1
}

# ============================================
# 5. Start Frontend
# ============================================
Write-Host "[5/6] Starting Frontend..." -ForegroundColor Yellow
try {
    Start-Process powershell -ArgumentList "-NoExit","-Command","cd `"$ClientDir`"; pnpm dev"
    Write-Host "  [OK] Frontend started in new window" -ForegroundColor Green
} catch {
    Write-Host "  [!] Frontend start failed" -ForegroundColor Red
}

# ============================================
# 6. Start Admin
# ============================================
Write-Host "[6/6] Starting Admin..." -ForegroundColor Yellow
try {
    Start-Process powershell -ArgumentList "-NoExit","-Command","cd `"$AdminDir`"; pnpm dev"
    Write-Host "  [OK] Admin started in new window" -ForegroundColor Green
} catch {
    Write-Host "  [!] Admin start failed" -ForegroundColor Red
}

# ============================================
# Done
# ============================================
Set-Location $ProjectRoot

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "    All services started!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Service URLs:" -ForegroundColor White
Write-Host "  - MySQL:    localhost:3306" -ForegroundColor Gray
Write-Host "  - Redis:    localhost:6379" -ForegroundColor Gray
Write-Host "  - MinIO:    http://127.0.0.1:9000 (Console: http://127.0.0.1:9001)" -ForegroundColor Gray
Write-Host "  - Backend:  http://localhost:8080" -ForegroundColor Gray
Write-Host "  - Frontend: http://localhost:5173" -ForegroundColor Gray
Write-Host "  - Admin:    http://localhost:5174" -ForegroundColor Gray
Write-Host ""
