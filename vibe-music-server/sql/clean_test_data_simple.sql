-- ============================================
-- 简化版：清理测试数据SQL脚本
-- 功能：快速删除测试用户及其所有相关数据
-- 注意：执行前请备份数据库！
-- ============================================

SET NAMES utf8mb4;

-- 临时禁用外键检查（加快删除速度，避免约束问题）
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 查看将要删除的测试数据（执行前检查）
-- ============================================
SELECT '测试用户数量:' AS info, COUNT(*) AS count 
FROM tb_user WHERE username LIKE 'testuser%' OR username LIKE 'test_user_%'
UNION ALL
SELECT '测试用户上传的歌曲:', COUNT(*) 
FROM tb_song s 
INNER JOIN tb_user u ON s.creator_id = u.id 
WHERE u.username LIKE 'testuser%' OR u.username LIKE 'test_user_%';

-- ============================================
-- 删除测试数据（使用临时表避免子查询问题）
-- ============================================

-- 创建临时表存储测试用户ID
CREATE TEMPORARY TABLE temp_test_user_ids AS
SELECT id FROM tb_user WHERE username LIKE 'testuser%' OR username LIKE 'test_user_%';

-- 删除测试用户的所有相关数据
DELETE f FROM tb_feedback f INNER JOIN temp_test_user_ids t ON f.user_id = t.id;
DELETE s FROM tb_song s INNER JOIN temp_test_user_ids t ON s.creator_id = t.id;
DELETE uf FROM tb_user_favorite uf INNER JOIN temp_test_user_ids t ON uf.user_id = t.id;
DELETE c FROM tb_comment c INNER JOIN temp_test_user_ids t ON c.user_id = t.id;
DELETE fr FROM tb_forum_reply fr INNER JOIN temp_test_user_ids t ON fr.user_id = t.id;
DELETE fp FROM tb_forum_post fp INNER JOIN temp_test_user_ids t ON fp.user_id = t.id;
DELETE u FROM tb_user u WHERE u.username LIKE 'testuser%' OR u.username LIKE 'test_user_%';

-- 删除临时表
DROP TEMPORARY TABLE temp_test_user_ids;

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 显示删除结果
SELECT '测试数据清理完成！' AS message;
SELECT '剩余测试用户数:' AS info, COUNT(*) AS count 
FROM tb_user WHERE username LIKE 'testuser%' OR username LIKE 'test_user_%';

