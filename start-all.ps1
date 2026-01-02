# ============================================
# MusicPlayer Start All Script
# ============================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "    Melody Hub Player Start Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Project root directory
$ProjectRoot = $PSScriptRoot
$ServerDir = Join-Path $ProjectRoot "vibe-music-server"
$ClientDir = Join-Path $ProjectRoot "vibe-music-client"
$AdminDir = Join-Path $ProjectRoot "vibe-music-admin"
$MinioExe = "D:\MinIO\minio.exe"
$MinioDataDir = "D:\MinIO\data"

# Function to clean up all backend Java processes
function Clean-BackendProcesses {
    Write-Host "  Cleaning up any existing backend processes..." -ForegroundColor Cyan
    try {
        $javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object {
            try {
                $wmiProcess = Get-WmiObject Win32_Process -Filter "ProcessId = $($_.Id)" -ErrorAction SilentlyContinue
                if ($wmiProcess -and ($wmiProcess.CommandLine -like "*vibe-music-server*" -or $wmiProcess.CommandLine -like "*spring-boot*")) {
                    return $true
                }
            } catch {
                return $false
            }
            return $false
        }
        
        if ($javaProcesses) {
            foreach ($proc in $javaProcesses) {
                Write-Host "  Stopping backend process: PID $($proc.Id)" -ForegroundColor Gray
                Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
            }
            Start-Sleep -Seconds 2
            Write-Host "  [OK] Cleaned up backend processes" -ForegroundColor Green
        } else {
            Write-Host "  [OK] No existing backend processes found" -ForegroundColor Green
        }
    } catch {
        Write-Host "  [!] Error cleaning up processes: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

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

# Clean up any existing backend processes first
Clean-BackendProcesses

# Function to check and free a port
function Free-Port {
    param([int]$Port, [int]$MaxRetries = 3)
    
    $retryCount = 0
    while ($retryCount -lt $MaxRetries) {
        $retryCount++
        Write-Host "  Checking port $Port (attempt $retryCount/$MaxRetries)..." -ForegroundColor Cyan
        
        try {
            $connection = Test-NetConnection -ComputerName localhost -Port $Port -WarningAction SilentlyContinue -InformationLevel Quiet -ErrorAction SilentlyContinue
            if (-not $connection) {
                Write-Host "  [OK] Port $Port is available" -ForegroundColor Green
                return $true
            }
            
            Write-Host "  [!] Port $Port is already in use" -ForegroundColor Yellow
            
            # Find all processes using this port
            $tcpConnections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
            if ($tcpConnections) {
                $pids = $tcpConnections | Select-Object -ExpandProperty OwningProcess -Unique
                foreach ($pid in $pids) {
                    try {
                        $process = Get-Process -Id $pid -ErrorAction SilentlyContinue
                        if ($process) {
                            Write-Host "  Found process: $($process.ProcessName) (PID: $pid)" -ForegroundColor Gray
                            
                            # Check if it's a Java process (likely our backend)
                            $isJavaProcess = $false
                            if ($process.ProcessName -eq "java") {
                                # Try to get command line to verify it's our backend
                                try {
                                    $wmiProcess = Get-WmiObject Win32_Process -Filter "ProcessId = $pid" -ErrorAction SilentlyContinue
                                    if ($wmiProcess -and ($wmiProcess.CommandLine -like "*vibe-music-server*" -or $wmiProcess.CommandLine -like "*spring-boot*")) {
                                        $isJavaProcess = $true
                                    }
                                } catch {
                                    # If we can't check command line, assume it's our backend if it's Java
                                    $isJavaProcess = $true
                                }
                            }
                            
                            if ($isJavaProcess) {
                                Write-Host "  Stopping Java process (likely previous backend instance)..." -ForegroundColor Yellow
                                Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
                                Start-Sleep -Seconds 2
                            } else {
                                Write-Host "  [!] Port $Port is used by non-Java process: $($process.ProcessName)" -ForegroundColor Yellow
                                Write-Host "  This might not be our backend. Proceeding anyway..." -ForegroundColor Yellow
                                # Still try to stop it if user wants
                                Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
                                Start-Sleep -Seconds 2
                            }
                        }
                    } catch {
                        Write-Host "  [!] Error stopping process $pid : $($_.Exception.Message)" -ForegroundColor Yellow
                    }
                }
                
                # Wait a bit and verify port is free
                Start-Sleep -Seconds 2
                $connectionAfter = Test-NetConnection -ComputerName localhost -Port $Port -WarningAction SilentlyContinue -InformationLevel Quiet -ErrorAction SilentlyContinue
                if (-not $connectionAfter) {
                    Write-Host "  [OK] Port $Port is now free" -ForegroundColor Green
                    return $true
                } else {
                    Write-Host "  [!] Port $Port is still in use after stopping processes" -ForegroundColor Yellow
                }
            } else {
                Write-Host "  [!] Could not identify processes using port $Port" -ForegroundColor Yellow
            }
        } catch {
            Write-Host "  [!] Error checking port $Port : $($_.Exception.Message)" -ForegroundColor Yellow
        }
        
        if ($retryCount -lt $MaxRetries) {
            Write-Host "  Retrying in 2 seconds..." -ForegroundColor Gray
            Start-Sleep -Seconds 2
        }
    }
    
    Write-Host "  [!] Failed to free port $Port after $MaxRetries attempts" -ForegroundColor Red
    return $false
}

# Check and free port 8080
$backendPort = 8080
$portFreed = Free-Port -Port $backendPort -MaxRetries 3

if (-not $portFreed) {
    Write-Host "  [!] Warning: Port $backendPort is still in use. Backend may fail to start." -ForegroundColor Red
    Write-Host "  Options:" -ForegroundColor Yellow
    Write-Host "    1. Manually stop the process using port $backendPort" -ForegroundColor Yellow
    Write-Host "    2. Change backend port in application.yml" -ForegroundColor Yellow
    Write-Host "  Continuing anyway..." -ForegroundColor Yellow
}

Write-Host "  Building backend project, please wait..." -ForegroundColor Cyan

Set-Location $ServerDir
mvn clean package -DskipTests

if ($LASTEXITCODE -eq 0) {
    Write-Host "  [OK] Backend build success" -ForegroundColor Green
    Write-Host "  Starting backend server in new window..." -ForegroundColor Cyan
    Start-Process powershell -ArgumentList "-NoExit","-Command","cd `"$ServerDir`"; java -jar target/vibe-music-server-0.0.1-SNAPSHOT.jar"
    Write-Host "  [OK] Backend server started" -ForegroundColor Green
    Write-Host "  [INFO] Backend is starting, please wait 20-30 seconds for initialization" -ForegroundColor Cyan
    Write-Host "  [INFO] Check the backend window for startup logs" -ForegroundColor Cyan
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
