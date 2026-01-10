# 500并发用户播放测试脚本

## 功能说明

这个脚本用于模拟500个用户同时播放歌曲，测试系统的并发性能。

## 测试流程

1. **用户登录**: 500个用户同时登录
2. **获取歌曲**: 获取歌曲列表或指定歌曲信息
3. **播放歌曲**: 模拟播放操作
4. **下载音频**: 从MinIO下载音频文件（模拟实际播放）

## 安装依赖

```bash
pip install -r requirements.txt
```

或者直接安装：

```bash
pip install aiohttp
```

## 配置说明

编辑 `concurrent_play_test.py` 文件，修改以下配置：

```python
BASE_URL = "http://localhost:8080"  # 后端API地址
MINIO_URL = "http://127.0.0.1:9000"  # MinIO地址
CONCURRENT_USERS = 500  # 并发用户数
TEST_DURATION = 60  # 测试持续时间（秒）
SONG_ID = None  # 指定歌曲ID，None则随机选择
```

## 使用前准备

### 1. 创建测试用户

需要提前创建500个测试用户账号。可以使用以下SQL脚本批量创建：

```sql
-- 批量创建测试用户
DELIMITER $$
CREATE PROCEDURE create_test_users()
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= 500 DO
        INSERT INTO tb_user (username, password, email, create_time, update_time, status)
        VALUES (
            CONCAT('testuser', i),
            MD5('123456'),  -- 密码需要MD5加密
            CONCAT('testuser', i, '@test.com'),
            NOW(),
            NOW(),
            0
        );
        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

-- 执行存储过程
CALL create_test_users();
```

或者使用Python脚本创建：

```python
import requests
import hashlib

BASE_URL = "http://localhost:8080"

for i in range(1, 501):
    username = f"testuser{i}"
    email = f"{username}@test.com"
    password = "123456"
    
    # 注册用户
    response = requests.post(f"{BASE_URL}/user/register", json={
        "username": username,
        "password": password,
        "email": email,
        "verificationCode": "000000"  # 需要先获取验证码
    })
    print(f"创建用户 {username}: {response.status_code}")
```

### 2. 确保服务运行

- 后端服务运行在 `http://localhost:8080`
- MinIO服务运行在 `http://127.0.0.1:9000`
- 数据库中有可播放的歌曲

## 运行测试

```bash
python concurrent_play_test.py
```

## 测试结果

脚本会输出以下统计信息：

- 总用户数
- 成功登录数及成功率
- 成功获取歌曲数及成功率
- 成功播放数及成功率
- 成功下载音频数及成功率
- 平均响应时间
- 最快/最慢响应时间
- 错误信息

## 性能优化建议

如果测试发现性能问题，可以考虑以下优化：

### 1. 后端优化

- **增加线程池大小**: 在 `application.yml` 中配置Tomcat线程池
- **数据库连接池**: 优化Druid连接池配置
- **Redis缓存**: 增加歌曲信息的缓存
- **CDN**: 将音频文件放到CDN

### 2. MinIO优化

- **增加MinIO节点**: 使用MinIO集群
- **增加带宽**: 确保网络带宽足够
- **缓存策略**: 在MinIO前增加缓存层

### 3. 数据库优化

- **索引优化**: 确保歌曲查询有合适的索引
- **读写分离**: 使用主从复制
- **连接池**: 优化数据库连接池大小

## 注意事项

1. **资源消耗**: 500并发会消耗大量系统资源，确保服务器配置足够
2. **网络带宽**: 音频文件下载会占用大量带宽，确保网络带宽充足
3. **数据库压力**: 大量并发查询会对数据库造成压力
4. **MinIO性能**: MinIO的IO性能直接影响音频下载速度

## 扩展测试

可以修改脚本来测试其他场景：

- 不同并发数（100, 200, 500, 1000）
- 不同测试时长
- 不同歌曲大小
- 混合操作（播放、收藏、评论等）

## 故障排查

如果测试失败，检查：

1. **网络连接**: 确保能访问后端和MinIO
2. **用户账号**: 确保测试用户已创建
3. **歌曲数据**: 确保数据库中有可播放的歌曲
4. **服务状态**: 确保所有服务正常运行
5. **日志**: 查看后端和MinIO日志

