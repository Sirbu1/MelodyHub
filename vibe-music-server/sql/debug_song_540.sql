-- 调试歌曲ID 540 为什么在曲库中找不到
-- 歌曲名称：不该相遇在秋天

-- 1. 查询歌曲的完整信息
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
    s.cover_url AS '封面URL',
    s.create_time AS '创建时间'
FROM tb_song s
LEFT JOIN tb_artist a ON s.artist_id = a.id
LEFT JOIN tb_user u ON s.creator_id = u.id
WHERE s.id = 540;

-- 2. 检查该歌曲是否符合曲库查询条件
SELECT 
    s.id AS '歌曲ID',
    s.name AS '歌曲名称',
    s.audit_status AS '审核状态',
    CASE 
        WHEN s.audit_status IS NULL THEN 'NULL（非原创）'
        WHEN s.audit_status = 0 THEN '待审核'
        WHEN s.audit_status = 1 THEN '已通过'
        WHEN s.audit_status = 2 THEN '未通过'
        ELSE '未知'
    END AS '审核状态说明',
    (s.audit_status IS NULL OR s.audit_status = 1) AS '是否符合查询条件',
    COALESCE(a.name, u.username) AS '艺术家/创建者'
FROM tb_song s
LEFT JOIN tb_artist a ON s.artist_id = a.id
LEFT JOIN tb_user u ON s.creator_id = u.id
WHERE s.id = 540;

-- 3. 模拟曲库查询（getSongsWithArtist方法）
SELECT 
    s.id AS songId, 
    s.name AS songName, 
    s.album, 
    s.duration, 
    s.cover_url AS coverUrl, 
    s.audio_url AS audioUrl, 
    s.release_time AS releaseTime, 
    COALESCE(a.name, u.username) AS artistName
FROM tb_song s
LEFT JOIN tb_artist a ON s.artist_id = a.id
LEFT JOIN tb_user u ON s.creator_id = u.id
WHERE 
    (NULL IS NULL OR s.name LIKE CONCAT('%', NULL, '%'))
    AND (NULL IS NULL OR COALESCE(a.name, u.username) LIKE CONCAT('%', NULL, '%'))
    AND (NULL IS NULL OR s.album LIKE CONCAT('%', NULL, '%'))
    AND (s.audit_status IS NULL OR s.audit_status = 1)
    AND s.id = 540;

-- 4. 检查是否有其他过滤条件
SELECT 
    s.id,
    s.name,
    s.audit_status,
    s.is_original,
    s.artist_id,
    s.creator_id,
    s.audio_url,
    CASE 
        WHEN s.audio_url IS NULL OR s.audio_url = '' THEN '音频URL为空'
        WHEN s.audio_url NOT LIKE 'http://%' AND s.audio_url NOT LIKE 'https://%' THEN '音频URL格式错误'
        ELSE '音频URL正常'
    END AS audioUrlStatus
FROM tb_song s
WHERE s.id = 540;

