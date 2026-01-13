-- ============================================
-- 清理测试数据SQL脚本
-- 功能：删除测试用户（testuser%、test_user_%）及其所有相关数据
-- 执行前请先备份数据库！
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. 查看将要删除的测试数据统计（执行前检查）
-- ============================================

-- 查看测试用户数量
SELECT 
    COUNT(*) AS test_user_count,
    GROUP_CONCAT(username ORDER BY id LIMIT 10) AS sample_usernames
FROM tb_user 
WHERE username LIKE 'testuser%' OR username LIKE 'test_user_%';

-- 查看测试用户上传的歌曲数量
SELECT 
    COUNT(*) AS test_song_count
FROM tb_song 
WHERE creator_id IN (
    SELECT id FROM tb_user WHERE username LIKE 'testuser%' OR username LIKE 'test_user_%'
);

-- 查看测试用户的收藏数量
SELECT 
    COUNT(*) AS test_favorite_count
FROM tb_user_favorite 
WHERE user_id IN (
    SELECT id FROM tb_user WHERE username LIKE 'testuser%' OR username LIKE 'test_user_%'
);

-- 查看测试用户的评论数量
SELECT 
    COUNT(*) AS test_comment_count
FROM tb_comment 
WHERE user_id IN (
    SELECT id FROM tb_user WHERE username LIKE 'testuser%' OR username LIKE 'test_user_%'
);

-- 查看测试用户的论坛帖子数量
SELECT 
    COUNT(*) AS test_post_count
FROM tb_forum_post 
WHERE user_id IN (
    SELECT id FROM tb_user WHERE username LIKE 'testuser%' OR username LIKE 'test_user_%'
);

-- 查看测试用户的论坛回复数量
SELECT 
    COUNT(*) AS test_reply_count
FROM tb_forum_reply 
WHERE user_id IN (
    SELECT id FROM tb_user WHERE username LIKE 'testuser%' OR username LIKE 'test_user_%'
);

-- ============================================
-- 2. 开始删除测试数据
-- ============================================

-- 注意：由于外键约束，删除顺序很重要
-- 先删除依赖数据，最后删除用户数据

-- 2.1 创建临时表存储测试用户ID（用于避免子查询问题）
CREATE TEMPORARY TABLE IF NOT EXISTS temp_test_user_ids AS
SELECT id FROM tb_user WHERE username LIKE 'testuser%' OR username LIKE 'test_user_%';

-- 2.2 删除测试用户的反馈（如果外键是RESTRICT，需要先删除）
-- 注意：tb_feedback的外键是RESTRICT，需要先删除
DELETE f FROM tb_feedback f
INNER JOIN temp_test_user_ids t ON f.user_id = t.id;

-- 2.3 删除测试用户上传的原创歌曲
-- 注意：tb_song表可能通过creator_id关联到测试用户
DELETE s FROM tb_song s
INNER JOIN temp_test_user_ids t ON s.creator_id = t.id;

-- 2.4 删除测试用户的收藏（虽然有CASCADE，但显式删除更安全）
DELETE uf FROM tb_user_favorite uf
INNER JOIN temp_test_user_ids t ON uf.user_id = t.id;

-- 2.5 删除测试用户的评论（虽然有CASCADE，但显式删除更安全）
DELETE c FROM tb_comment c
INNER JOIN temp_test_user_ids t ON c.user_id = t.id;

-- 2.6 删除测试用户的论坛回复
DELETE fr FROM tb_forum_reply fr
INNER JOIN temp_test_user_ids t ON fr.user_id = t.id;

-- 2.7 删除测试用户的论坛帖子
-- 注意：删除帖子前，需要先删除相关的回复（已在上一步完成）
DELETE fp FROM tb_forum_post fp
INNER JOIN temp_test_user_ids t ON fp.user_id = t.id;

-- 2.8 删除测试用户相关的接单记录（如果tb_forum_order表存在）
-- 注意：如果表不存在，此语句会报错，可以注释掉或先检查表是否存在
-- DELETE fo FROM tb_forum_order fo
-- INNER JOIN temp_test_user_ids t ON fo.user_id = t.id;

-- 2.9 最后删除测试用户（CASCADE会自动删除其他关联数据）
DELETE u FROM tb_user u
WHERE u.username LIKE 'testuser%' OR u.username LIKE 'test_user_%';

-- 2.10 删除临时表
DROP TEMPORARY TABLE IF EXISTS temp_test_user_ids;

-- ============================================
-- 3. 删除其他测试歌曲（通过名称或特征识别）
-- ============================================

-- 删除名称包含"test"、"测试"的歌曲（根据实际情况调整）
-- 注意：此操作需要谨慎，确保不会误删正常歌曲
-- DELETE FROM tb_song 
-- WHERE name LIKE '%test%' 
--    OR name LIKE '%测试%'
--    OR name LIKE '%Test%';

-- ============================================
-- 4. 验证删除结果
-- ============================================

-- 检查是否还有测试用户
SELECT 
    COUNT(*) AS remaining_test_users
FROM tb_user 
WHERE username LIKE 'testuser%' OR username LIKE 'test_user_%';

-- 检查是否还有测试用户的歌曲（此时测试用户已删除，应该为0）
SELECT 
    COUNT(*) AS remaining_test_songs
FROM tb_song s
WHERE EXISTS (
    SELECT 1 FROM tb_user u 
    WHERE u.id = s.creator_id 
    AND (u.username LIKE 'testuser%' OR u.username LIKE 'test_user_%')
);

-- ============================================
-- 5. 恢复外键检查
-- ============================================

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 执行完成提示
-- ============================================
SELECT '测试数据清理完成！' AS message;

