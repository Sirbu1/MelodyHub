# 批量创建测试用户指南

## 方法1: 使用 SQL 脚本（推荐）

### 步骤1: 连接到数据库

```bash
mysql -u root -p3127 vibe_music
```

或者：

```bash
mysql -u root -p
# 然后输入密码
use vibe_music;
```

### 步骤2: 执行 SQL 脚本

**方式A: 在 MySQL 客户端中执行**

```sql
source scripts/create_test_users.sql
```

或者直接复制 SQL 脚本内容到 MySQL 客户端执行。

**方式B: 使用命令行执行**

```bash
mysql -u root -p3127 vibe_music < scripts/create_test_users.sql
```

### 步骤3: 验证结果

```sql
-- 查看创建的测试用户数量
SELECT COUNT(*) AS total_test_users 
FROM tb_user 
WHERE username LIKE 'testuser%';

-- 查看前10个用户
SELECT id, username, email, create_time, score
FROM tb_user 
WHERE username LIKE 'testuser%'
ORDER BY id
LIMIT 10;
```

## 方法2: 使用 PowerShell 脚本

### 步骤1: 修改配置

编辑 `create_test_users.ps1`，修改数据库连接信息：

```powershell
$dbHost = "localhost"
$dbPort = "3306"
$dbName = "vibe_music"
$dbUser = "root"
$dbPassword = "3127"  # 修改为你的密码
$userCount = 500
```

### 步骤2: 执行脚本

```powershell
cd vibe-music-server\scripts
.\create_test_users.ps1
```

## 方法3: 使用 Python 脚本（通过 API）

如果不想直接操作数据库，可以通过 API 注册用户：

```python
import requests
import hashlib
import time

BASE_URL = "http://localhost:8080"

def register_user(username, password, email):
    """注册用户"""
    # 注意：需要先获取验证码，这里假设验证码为 000000
    # 实际使用时需要先调用 /user/sendVerificationCode 获取验证码
    response = requests.post(f"{BASE_URL}/user/register", json={
        "username": username,
        "password": password,
        "email": email,
        "verificationCode": "000000"  # 需要先获取验证码
    })
    return response.status_code == 200 and response.json().get("code") == 0

# 批量创建用户
for i in range(1, 501):
    username = f"testuser{i}"
    email = f"{username}@test.com"
    password = "123456"
    
    if register_user(username, password, email):
        print(f"✓ 创建用户 {username}")
    else:
        print(f"✗ 创建用户 {username} 失败")
    
    time.sleep(0.1)  # 避免请求过快
```

## 方法4: 直接使用 INSERT 语句（快速但需要手动处理）

如果只需要少量用户，可以直接使用 INSERT 语句：

```sql
INSERT INTO tb_user (username, password, email, create_time, update_time, status, score)
VALUES 
    ('testuser1', 'e10adc3949ba59abbe56e057f20f883e', 'testuser1@test.com', NOW(), NOW(), 0, 100),
    ('testuser2', 'e10adc3949ba59abbe56e057f20f883e', 'testuser2@test.com', NOW(), NOW(), 0, 100),
    -- ... 更多用户
    ('testuser500', 'e10adc3949ba59abbe56e057f20f883e', 'testuser500@test.com', NOW(), NOW(), 0, 100);
```

## 用户信息说明

- **用户名**: testuser1, testuser2, ..., testuser500
- **密码**: 123456 (MD5: e10adc3949ba59abbe56e057f20f883e)
- **邮箱**: testuser1@test.com, testuser2@test.com, ...
- **初始积分**: 100
- **状态**: 0 (启用)

## 验证密码

密码 `123456` 的 MD5 值是 `e10adc3949ba59abbe56e057f20f883e`

可以使用以下 SQL 验证：

```sql
SELECT MD5('123456');  -- 应该返回: e10adc3949ba59abbe56e057f20f883e
```

## 清理测试用户

如果需要删除测试用户：

```sql
DELETE FROM tb_user WHERE username LIKE 'testuser%';
```

## 常见问题

### 1. 用户已存在错误

如果用户已存在，存储过程会跳过（使用 `IF NOT EXISTS` 检查）。

### 2. 密码加密问题

确保密码使用 MD5 加密。如果后端使用其他加密方式，需要相应调整。

### 3. 权限问题

确保数据库用户有 INSERT 权限。

### 4. 字符集问题

如果出现中文乱码，确保数据库和表的字符集为 `utf8mb4`。

## 测试登录

创建用户后，可以使用以下方式测试登录：

```bash
curl -X POST http://localhost:8080/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser1","password":"123456"}'
```

或者使用 Python：

```python
import requests

response = requests.post("http://localhost:8080/user/login", json={
    "username": "testuser1",
    "password": "123456"
})

print(response.json())
```

