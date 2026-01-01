# SQL 迁移文件执行脚本
# 用途：自动执行 add_audit_status_fields.sql 迁移文件

param(
    [string]$MysqlPath = "mysql",
    [string]$Username = "root",
    [string]$Password = "3127",
    [string]$Database = "vibe_music",
    [string]$SqlFile = "add_audit_status_fields.sql"
)

# 获取脚本所在目录
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$SqlFilePath = Join-Path $ScriptDir $SqlFile

# 检查 SQL 文件是否存在
if (-not (Test-Path $SqlFilePath)) {
    Write-Host "错误：找不到 SQL 文件: $SqlFilePath" -ForegroundColor Red
    exit 1
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "SQL 迁移文件执行工具" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "数据库: $Database" -ForegroundColor White
Write-Host "SQL 文件: $SqlFilePath" -ForegroundColor White
Write-Host ""

# 检查 MySQL 是否可用
try {
    $null = Get-Command $MysqlPath -ErrorAction Stop
} catch {
    Write-Host "错误：找不到 MySQL 命令: $MysqlPath" -ForegroundColor Red
    Write-Host "提示：请将 MySQL bin 目录添加到 PATH，或使用 -MysqlPath 参数指定完整路径" -ForegroundColor Yellow
    Write-Host "例如: -MysqlPath 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'" -ForegroundColor Yellow
    exit 1
}

# 询问是否继续
$confirmation = Read-Host "是否继续执行迁移？(Y/N)"
if ($confirmation -ne 'Y' -and $confirmation -ne 'y') {
    Write-Host "已取消执行" -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "正在执行 SQL 迁移文件..." -ForegroundColor Yellow

# 执行 SQL 文件
$ErrorActionPreference = "Stop"
try {
    # 读取 SQL 文件内容
    $sqlContent = Get-Content $SqlFilePath -Raw -Encoding UTF8
    
    # 执行 SQL
    $sqlContent | & $MysqlPath -u $Username -p$Password $Database
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✓ SQL 迁移执行成功！" -ForegroundColor Green
        Write-Host ""
        Write-Host "验证建议：" -ForegroundColor Cyan
        Write-Host "  执行以下 SQL 查询来验证字段是否添加成功：" -ForegroundColor White
        Write-Host "  DESCRIBE tb_song;" -ForegroundColor Gray
        Write-Host "  DESCRIBE tb_forum_post;" -ForegroundColor Gray
        Write-Host "  DESCRIBE tb_forum_reply;" -ForegroundColor Gray
        Write-Host "  DESCRIBE tb_comment;" -ForegroundColor Gray
    } else {
        Write-Host ""
        Write-Host "✗ SQL 迁移执行失败！退出代码: $LASTEXITCODE" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host ""
    Write-Host "✗ 执行过程中发生错误：" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}

