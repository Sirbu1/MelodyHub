-- 简单方式创建500个测试用户
-- 密码: 123456 的 MD5 值: e10adc3949ba59abbe56e057f20f883e

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

SELECT COUNT(*) AS created_users FROM tb_user WHERE username LIKE 'testuser%';

