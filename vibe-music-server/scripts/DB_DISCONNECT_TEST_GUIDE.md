# 数据库断连测试指南

## 测试目标

1. 模拟100用户进行收藏、评论操作
2. 手动断开数据库连接，持续1分钟
3. 观察用户操作反馈
4. 恢复数据库连接，等待系统重连
5. 检查断连期间的操作数据是否完整

## 测试步骤

### 1. 准备测试环境

确保以下服务运行：
- 后端服务: http://localhost:8080
- MySQL服务: localhost:3306
- 测试用户已创建（500个testuser账号）

### 2. 运行测试脚本

```bash
cd vibe-music-server/scripts
python db_disconnect_test_final.py
```

### 3. 手动断开数据库（在另一个终端）

**Windows:**
```powershell
# 以管理员身份运行
net stop MySQL
```

**Linux/Mac:**
```bash
sudo systemctl stop mysql
# 或
sudo service mysql stop
```

### 4. 等待60秒

观察测试脚本的输出，查看：
- 操作成功率变化
- 错误信息
- 响应时间变化

### 5. 恢复数据库连接

**Windows:**
```powershell
# 以管理员身份运行
net start MySQL
```

**Linux/Mac:**
```bash
sudo systemctl start mysql
# 或
sudo service mysql start
```

### 6. 等待测试完成

脚本会自动：
- 检测数据库连接恢复
- 统计所有操作结果
- 检查数据完整性

## 预期结果

### 正常情况（数据库连接正常）
- 操作成功率: > 95%
- 平均响应时间: < 0.1秒
- 数据完整性: 100%

### 数据库断连期间
- 操作成功率: 可能下降（取决于连接池和重试机制）
- 响应时间: 可能增加（超时等待）
- 错误类型: 数据库连接错误、超时错误

### 数据库恢复后
- 系统自动重连
- 断连期间失败的操作可能需要重试
- 数据完整性检查通过

## 数据完整性验证

脚本会检查：
1. **重复数据**: 是否有重复的收藏或评论记录
2. **数据丢失**: 成功操作的数据是否都保存到数据库
3. **数据一致性**: 预期数据量 vs 实际数据量

## 注意事项

1. **需要管理员权限**: 停止/启动MySQL服务需要管理员权限
2. **测试时长**: 建议测试时长为120秒，确保有足够时间断开和恢复数据库
3. **数据清理**: 测试会产生大量数据，测试后可能需要清理
4. **服务影响**: 停止MySQL会影响所有使用该数据库的应用

## 清理测试数据（可选）

```sql
-- 删除测试收藏数据
DELETE FROM tb_user_favorite 
WHERE user_id IN (SELECT id FROM tb_user WHERE username LIKE 'testuser%')
AND create_time >= DATE_SUB(NOW(), INTERVAL 10 MINUTE);

-- 删除测试评论数据
DELETE FROM tb_comment 
WHERE user_id IN (SELECT id FROM tb_user WHERE username LIKE 'testuser%')
AND create_time >= DATE_SUB(NOW(), INTERVAL 10 MINUTE);
```

