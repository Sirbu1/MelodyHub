# SQL 迁移文件执行说明

## 文件说明
- `add_audit_status_fields.sql` - 添加审核状态字段的迁移脚本

## 执行方法

### 方法一：使用 MySQL 命令行工具（推荐）

1. **打开命令提示符或 PowerShell**

2. **登录 MySQL**
   ```bash
   mysql -u root -p
   ```
   输入密码：`3127`（根据你的实际配置）

3. **选择数据库**
   ```sql
   USE vibe_music;
   ```

4. **执行 SQL 文件**
   ```sql
   SOURCE D:/15591/Desktop/MusicPlayer/vibe-music-server/sql/add_audit_status_fields.sql;
   ```
   或者使用相对路径：
   ```sql
   SOURCE sql/add_audit_status_fields.sql;
   ```

5. **验证执行结果**
   ```sql
   -- 检查字段是否添加成功
   DESCRIBE tb_song;
   DESCRIBE tb_forum_post;
   DESCRIBE tb_forum_reply;
   DESCRIBE tb_comment;
   ```

### 方法二：使用命令行直接执行（无需登录）

在 PowerShell 或命令提示符中执行：

```powershell
# Windows PowerShell
mysql -u root -p3127 vibe_music < D:\15591\Desktop\MusicPlayer\vibe-music-server\sql\add_audit_status_fields.sql
```

或者：

```bash
# 如果密码包含特殊字符，使用 -p 参数（会提示输入密码）
mysql -u root -p vibe_music < D:\15591\Desktop\MusicPlayer\vibe-music-server\sql\add_audit_status_fields.sql
```

### 方法三：使用 MySQL Workbench

1. **打开 MySQL Workbench**
2. **连接到数据库**（localhost:3306, 用户：root）
3. **打开 SQL 文件**
   - 点击 `File` → `Open SQL Script`
   - 选择 `add_audit_status_fields.sql` 文件
4. **选择数据库**
   - 在 SQL 编辑器中，确保选择了 `vibe_music` 数据库
   - 或者执行：`USE vibe_music;`
5. **执行脚本**
   - 点击 `Execute` 按钮（或按 `Ctrl+Shift+Enter`）

### 方法四：使用 Navicat、DBeaver 等图形化工具

1. **打开数据库管理工具**（Navicat、DBeaver、phpMyAdmin 等）
2. **连接到数据库**
   - 主机：`localhost`
   - 端口：`3306`
   - 用户名：`root`
   - 密码：`3127`
   - 数据库：`vibe_music`
3. **执行 SQL 文件**
   - 打开 SQL 编辑器
   - 导入或打开 `add_audit_status_fields.sql` 文件
   - 执行脚本

### 方法五：使用 PowerShell 脚本（自动化）

创建一个 PowerShell 脚本来自动执行：

```powershell
# execute-migration.ps1
$mysqlPath = "mysql"  # 如果 MySQL 不在 PATH 中，使用完整路径，如 "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
$username = "root"
$password = "3127"
$database = "vibe_music"
$sqlFile = "D:\15591\Desktop\MusicPlayer\vibe-music-server\sql\add_audit_status_fields.sql"

Write-Host "正在执行 SQL 迁移文件..." -ForegroundColor Yellow
& $mysqlPath -u $username -p$password $database -e "source $sqlFile"

if ($LASTEXITCODE -eq 0) {
    Write-Host "SQL 迁移执行成功！" -ForegroundColor Green
} else {
    Write-Host "SQL 迁移执行失败！" -ForegroundColor Red
}
```

## 验证迁移结果

执行完成后，可以运行以下 SQL 查询来验证：

```sql
-- 检查字段是否存在
SHOW COLUMNS FROM tb_song LIKE 'audit_status';
SHOW COLUMNS FROM tb_forum_post LIKE 'audit_status';
SHOW COLUMNS FROM tb_forum_reply LIKE 'audit_status';
SHOW COLUMNS FROM tb_comment LIKE 'audit_status';

-- 检查索引是否存在
SHOW INDEX FROM tb_song WHERE Key_name = 'idx_audit_status';
SHOW INDEX FROM tb_forum_post WHERE Key_name = 'idx_audit_status';
SHOW INDEX FROM tb_forum_reply WHERE Key_name = 'idx_audit_status';
SHOW INDEX FROM tb_comment WHERE Key_name = 'idx_audit_status';

-- 检查现有数据的审核状态
SELECT COUNT(*) as total, 
       SUM(CASE WHEN audit_status = 0 THEN 1 ELSE 0 END) as pending,
       SUM(CASE WHEN audit_status = 1 THEN 1 ELSE 0 END) as approved,
       SUM(CASE WHEN audit_status = 2 THEN 1 ELSE 0 END) as rejected
FROM tb_song;
```

## 注意事项

1. **备份数据库**：在执行迁移前，建议先备份数据库
   ```sql
   -- 使用 mysqldump 备份
   mysqldump -u root -p3127 vibe_music > vibe_music_backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql
   ```

2. **检查现有数据**：迁移脚本会将现有数据的 `audit_status` 设置为 1（已通过），确保现有内容可以正常显示

3. **如果字段已存在**：如果字段已经存在，执行 ALTER TABLE 会报错。可以先检查：
   ```sql
   SELECT COLUMN_NAME 
   FROM INFORMATION_SCHEMA.COLUMNS 
   WHERE TABLE_SCHEMA = 'vibe_music' 
   AND TABLE_NAME = 'tb_song' 
   AND COLUMN_NAME = 'audit_status';
   ```

4. **回滚方案**：如果需要回滚，可以执行以下 SQL：
   ```sql
   ALTER TABLE `tb_song` DROP COLUMN `audit_status`;
   ALTER TABLE `tb_forum_post` DROP COLUMN `audit_status`;
   ALTER TABLE `tb_forum_reply` DROP COLUMN `audit_status`;
   ALTER TABLE `tb_comment` DROP COLUMN `audit_status`;
   ```

## 常见问题

### 问题1：找不到 mysql 命令
**解决方案**：将 MySQL 的 bin 目录添加到系统 PATH 环境变量中，或使用完整路径。

### 问题2：Access denied 错误
**解决方案**：检查用户名和密码是否正确，确保用户有足够的权限。

### 问题3：Table doesn't exist 错误
**解决方案**：确保数据库 `vibe_music` 已创建，并且表已存在。

### 问题4：Duplicate column name 错误
**解决方案**：字段已存在，可以跳过该 ALTER TABLE 语句，或先删除字段再执行。

