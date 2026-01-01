-- 检查歌曲审核状态和关联信息
-- 用于排查为什么审核通过的歌曲在曲库中找不到

-- 1. 查看所有原创歌曲及其审核状态
SELECT 
    s.id AS '歌曲ID',
    s.name AS '歌曲名称',
    s.is_original AS '是否原创',
    s.audit_status AS '审核状态',
    s.artist_id AS '艺术家ID',
    s.creator_id AS '创建者ID',
    a.name AS '艺术家名称',
    u.username AS '创建者名称',
    s.audio_url AS '音频URL',
    s.create_time AS '创建时间'
FROM tb_song s
LEFT JOIN tb_artist a ON s.artist_id = a.id
LEFT JOIN tb_user u ON s.creator_id = u.id
WHERE s.is_original = true
ORDER BY s.create_time DESC
LIMIT 20;

-- 2. 查看审核通过的原创歌曲（应该出现在曲库中）
SELECT 
    s.id AS '歌曲ID',
    s.name AS '歌曲名称',
    s.audit_status AS '审核状态',
    s.artist_id AS '艺术家ID',
    s.creator_id AS '创建者ID',
    a.name AS '艺术家名称',
    u.username AS '创建者名称'
FROM tb_song s
LEFT JOIN tb_artist a ON s.artist_id = a.id
LEFT JOIN tb_user u ON s.creator_id = u.id
WHERE s.is_original = true 
  AND (s.audit_status = 1 OR s.audit_status IS NULL)
ORDER BY s.create_time DESC;

-- 3. 查看待审核的原创歌曲
SELECT 
    s.id AS '歌曲ID',
    s.name AS '歌曲名称',
    s.audit_status AS '审核状态',
    s.creator_id AS '创建者ID',
    u.username AS '创建者名称',
    s.create_time AS '上传时间'
FROM tb_song s
LEFT JOIN tb_user u ON s.creator_id = u.id
WHERE s.is_original = true 
  AND s.audit_status = 0
ORDER BY s.create_time DESC;

-- 4. 检查特定歌曲的详细信息（替换 YOUR_SONG_ID 为实际歌曲ID）
-- SELECT 
--     s.*,
--     a.name AS artist_name,
--     u.username AS creator_name
-- FROM tb_song s
-- LEFT JOIN tb_artist a ON s.artist_id = a.id
-- LEFT JOIN tb_user u ON s.creator_id = u.id
-- WHERE s.id = YOUR_SONG_ID;

