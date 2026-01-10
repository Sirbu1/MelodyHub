-- ============================================
-- 批量创建500个测试用户脚本
-- ============================================
-- 使用方法：
--   1. 连接到数据库: mysql -u root -p vibe_music
--   2. 执行此脚本: source create_test_users.sql
--   或者直接在MySQL客户端中执行以下SQL语句
-- ============================================

-- 方法1: 使用存储过程（推荐）
DELIMITER $$

DROP PROCEDURE IF EXISTS create_test_users$$

CREATE PROCEDURE create_test_users()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE username VARCHAR(50);
    DECLARE email VARCHAR(100);
    DECLARE password_md5 VARCHAR(32);
    
    -- 开始事务
    START TRANSACTION;
    
    WHILE i <= 500 DO
        SET username = CONCAT('testuser', i);
        SET email = CONCAT('testuser', i, '@test.com');
        -- 密码: 123456 的 MD5 值
        SET password_md5 = 'e10adc3949ba59abbe56e057f20f883e';
        
        -- 检查用户是否已存在
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
                0,  -- 状态: 0-启用
                100 -- 初始积分: 100
            );
        END IF;
        
        SET i = i + 1;
    END WHILE;
    
    -- 提交事务
    COMMIT;
    
    SELECT CONCAT('成功创建 ', COUNT(*), ' 个测试用户') AS result
    FROM tb_user 
    WHERE username LIKE 'testuser%';
END$$

DELIMITER ;

-- 执行存储过程
CALL create_test_users();

-- 查看创建结果
SELECT 
    COUNT(*) AS total_test_users,
    MIN(create_time) AS first_created,
    MAX(create_time) AS last_created
FROM tb_user 
WHERE username LIKE 'testuser%';

-- 显示前10个用户
SELECT 
    id,
    username,
    email,
    create_time,
    score
FROM tb_user 
WHERE username LIKE 'testuser%'
ORDER BY id
LIMIT 10;

-- ============================================
-- 方法2: 使用循环插入（如果存储过程不可用）
-- ============================================
-- 注意：MySQL 8.0+ 支持，如果版本较低可能需要使用存储过程
/*
SET @i = 1;
WHILE @i <= 500 DO
    INSERT INTO tb_user (
        username, 
        password, 
        email, 
        create_time, 
        update_time, 
        status,
        score
    ) VALUES (
        CONCAT('testuser', @i),
        'e10adc3949ba59abbe56e057f20f883e',  -- 123456 的 MD5
        CONCAT('testuser', @i, '@test.com'),
        NOW(),
        NOW(),
        0,
        100
    )
    ON DUPLICATE KEY UPDATE username = username;  -- 如果已存在则跳过
    
    SET @i = @i + 1;
END WHILE;
*/

-- ============================================
-- 方法3: 使用批量INSERT（一次性插入，最快）
-- ============================================
-- 注意：如果用户表有唯一约束，需要先检查是否已存在
/*
INSERT INTO tb_user (username, password, email, create_time, update_time, status, score)
SELECT 
    CONCAT('testuser', n) AS username,
    'e10adc3949ba59abbe56e057f20f883e' AS password,
    CONCAT('testuser', n, '@test.com') AS email,
    NOW() AS create_time,
    NOW() AS update_time,
    0 AS status,
    100 AS score
FROM (
    SELECT @row := @row + 1 AS n
    FROM (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) t3,
         (SELECT @row := 0) r
    LIMIT 500
) numbers
WHERE NOT EXISTS (
    SELECT 1 FROM tb_user WHERE username = CONCAT('testuser', n)
);
*/

-- ============================================
-- 删除测试用户（如果需要清理）
-- ============================================
/*
DELETE FROM tb_user WHERE username LIKE 'testuser%';
*/

-- ============================================
-- 验证用户密码
-- ============================================
-- 密码: 123456
-- MD5: e10adc3949ba59abbe56e057f20f883e
-- 可以使用以下SQL验证：
-- SELECT MD5('123456');  -- 应该返回: e10adc3949ba59abbe56e057f20f883e

