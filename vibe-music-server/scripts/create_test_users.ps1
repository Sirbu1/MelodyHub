# ============================================
# PowerShell 脚本：批量创建测试用户
# ============================================
# 使用方法：
#   1. 修改下面的数据库连接信息
#   2. 在 PowerShell 中执行: .\create_test_users.ps1
# ============================================

# 数据库配置
$dbHost = "localhost"
$dbPort = "3306"
$dbName = "vibe_music"
$dbUser = "root"
$dbPassword = "3127"  # 修改为你的数据库密码

# 测试用户数量
$userCount = 500

# 构建 MySQL 连接字符串
$connectionString = "server=$dbHost;port=$dbPort;database=$dbName;uid=$dbUser;pwd=$dbPassword"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "批量创建测试用户脚本" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "数据库: $dbName" -ForegroundColor Yellow
Write-Host "用户数量: $userCount" -ForegroundColor Yellow
Write-Host "开始时间: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Cyan

try {
    # 检查是否安装了 MySQL .NET Connector
    # 如果没有，可以使用 mysql 命令行工具
    $mysqlPath = "mysql"
    
    # 检查 mysql 命令是否可用
    $mysqlAvailable = Get-Command mysql -ErrorAction SilentlyContinue
    
    if (-not $mysqlAvailable) {
        Write-Host "错误: 未找到 mysql 命令" -ForegroundColor Red
        Write-Host "请确保 MySQL 客户端已安装并添加到 PATH" -ForegroundColor Red
        Write-Host "或者使用 SQL 脚本直接执行: create_test_users.sql" -ForegroundColor Yellow
        exit 1
    }
    
    # 创建临时 SQL 文件
    $tempSqlFile = [System.IO.Path]::GetTempFileName()
    $sqlContent = @"
-- 批量创建测试用户
DELIMITER $$

DROP PROCEDURE IF EXISTS create_test_users$$

CREATE PROCEDURE create_test_users()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE username VARCHAR(50);
    DECLARE email VARCHAR(100);
    DECLARE password_md5 VARCHAR(32);
    
    START TRANSACTION;
    
    WHILE i <= $userCount DO
        SET username = CONCAT('testuser', i);
        SET email = CONCAT('testuser', i, '@test.com');
        SET password_md5 = 'e10adc3949ba59abbe56e057f20f883e';
        
        IF NOT EXISTS (SELECT 1 FROM tb_user WHERE username = username) THEN
            INSERT INTO tb_user (
                username, 
                password, 
                email, 
                create_time, 
                update_time, 
                status,
                score
            ) VALUES (
                username,
                password_md5,
                email,
                NOW(),
                NOW(),
                0,
                100
            );
        END IF;
        
        SET i = i + 1;
    END WHILE;
    
    COMMIT;
    
    SELECT CONCAT('成功创建 ', COUNT(*), ' 个测试用户') AS result
    FROM tb_user 
    WHERE username LIKE 'testuser%';
END$$

DELIMITER ;

CALL create_test_users();

SELECT 
    COUNT(*) AS total_test_users,
    MIN(create_time) AS first_created,
    MAX(create_time) AS last_created
FROM tb_user 
WHERE username LIKE 'testuser%';
"@
    
    $sqlContent | Out-File -FilePath $tempSqlFile -Encoding UTF8
    
    Write-Host "`n正在执行 SQL 脚本..." -ForegroundColor Green
    
    # 执行 SQL 脚本
    $mysqlCommand = "mysql -h $dbHost -P $dbPort -u $dbUser -p$dbPassword $dbName < `"$tempSqlFile`""
    
    # 使用 cmd 执行（因为 PowerShell 的管道可能有问题）
    $result = cmd /c "mysql -h $dbHost -P $dbPort -u $dbUser -p$dbPassword $dbName < `"$tempSqlFile`" 2>&1"
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "`n成功创建测试用户！" -ForegroundColor Green
        Write-Host $result -ForegroundColor White
    } else {
        Write-Host "`n执行过程中出现错误:" -ForegroundColor Red
        Write-Host $result -ForegroundColor Red
    }
    
    # 清理临时文件
    Remove-Item $tempSqlFile -ErrorAction SilentlyContinue
    
} catch {
    Write-Host "`n错误: $_" -ForegroundColor Red
    Write-Host "堆栈跟踪: $($_.ScriptStackTrace)" -ForegroundColor Red
}

Write-Host "`n结束时间: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Cyan

