-- 修复缺失的原创歌手记录
-- 此脚本会为所有已通过审核的原创歌曲的创建者创建或更新歌手记录

-- 1. 查看所有已通过审核的原创歌曲及其创建者
SELECT 
    s.id AS '歌曲ID',
    s.name AS '歌曲名称',
    s.creator_id AS '创建者ID',
    u.username AS '创建者用户名',
    u.birth AS '生日',
    u.area AS '国籍',
    u.introduction AS '简介',
    u.user_avatar AS '头像',
    a.id AS '已有歌手ID',
    a.name AS '已有歌手名称'
FROM tb_song s
INNER JOIN tb_user u ON s.creator_id = u.id
LEFT JOIN tb_artist a ON u.username = a.name AND a.gender = 3
WHERE s.is_original = true 
  AND (s.audit_status = 1 OR s.audit_status IS NULL)
ORDER BY s.create_time DESC;

-- 2. 为所有已通过审核的原创歌曲的创建者创建或更新歌手记录
-- 注意：此操作会为每个用户创建一条歌手记录（如果不存在）
INSERT INTO tb_artist (name, gender, birth, area, introduction, avatar)
SELECT DISTINCT
    u.username AS name,
    3 AS gender,  -- 原创歌手
    u.birth,
    u.area,
    u.introduction,
    u.user_avatar AS avatar
FROM tb_song s
INNER JOIN tb_user u ON s.creator_id = u.id
LEFT JOIN tb_artist a ON u.username = a.name AND a.gender = 3
WHERE s.is_original = true 
  AND (s.audit_status = 1 OR s.audit_status IS NULL)
  AND a.id IS NULL  -- 只插入不存在的歌手
  AND u.username IS NOT NULL 
  AND u.username != '';

-- 3. 更新已存在的歌手信息（同步用户的最新信息）
UPDATE tb_artist a
INNER JOIN tb_user u ON a.name = u.username AND a.gender = 3
INNER JOIN tb_song s ON u.id = s.creator_id
SET 
    a.birth = u.birth,
    a.area = u.area,
    a.introduction = u.introduction,
    a.avatar = COALESCE(u.user_avatar, a.avatar)
WHERE s.is_original = true 
  AND (s.audit_status = 1 OR s.audit_status IS NULL);

-- 4. 验证结果：查看所有原创歌手
SELECT 
    a.id AS '歌手ID',
    a.name AS '歌手名称',
    a.gender AS '类型',
    a.birth AS '生日',
    a.area AS '国籍',
    COUNT(DISTINCT s.id) AS '已通过审核的歌曲数'
FROM tb_artist a
LEFT JOIN tb_user u ON a.name = u.username
LEFT JOIN tb_song s ON u.id = s.creator_id 
    AND s.is_original = true 
    AND (s.audit_status = 1 OR s.audit_status IS NULL)
WHERE a.gender = 3
GROUP BY a.id, a.name, a.gender, a.birth, a.area
ORDER BY a.id DESC;

