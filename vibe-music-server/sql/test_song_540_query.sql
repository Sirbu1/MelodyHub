-- 测试歌曲ID 540 在不同查询条件下的结果
-- 歌曲名称：不该相遇在秋天

-- ============================================
-- 1. 模拟用户端查询（getSongsWithArtist）
-- ============================================
-- 用户端查询：无搜索条件，第一页，每页20条
SELECT 
    s.id AS songId, 
    s.name AS songName, 
    s.album, 
    s.duration, 
    s.cover_url AS coverUrl, 
    s.audio_url AS audioUrl, 
    s.release_time AS releaseTime, 
    COALESCE(a.name, u.username) AS artistName,
    s.audit_status AS auditStatus
FROM tb_song s
LEFT JOIN tb_artist a ON s.artist_id = a.id
LEFT JOIN tb_user u ON s.creator_id = u.id
WHERE 
    (NULL IS NULL OR s.name LIKE CONCAT('%', NULL, '%'))
    AND (NULL IS NULL OR COALESCE(a.name, u.username) LIKE CONCAT('%', NULL, '%'))
    AND (NULL IS NULL OR s.album LIKE CONCAT('%', NULL, '%'))
    AND (s.audit_status IS NULL OR s.audit_status = 1)
ORDER BY s.id DESC
LIMIT 20;

-- 检查歌曲540是否在结果中
SELECT 
    s.id AS songId, 
    s.name AS songName, 
    s.audit_status AS auditStatus,
    (s.audit_status IS NULL OR s.audit_status = 1) AS '是否符合查询条件',
    COALESCE(a.name, u.username) AS artistName
FROM tb_song s
LEFT JOIN tb_artist a ON s.artist_id = a.id
LEFT JOIN tb_user u ON s.creator_id = u.id
WHERE s.id = 540
    AND (s.audit_status IS NULL OR s.audit_status = 1);

-- ============================================
-- 2. 模拟管理端查询（getSongsWithArtistName）
-- ============================================
-- 管理端查询：无搜索条件，第一页，每页20条
SELECT 
    s.id AS songId, 
    s.name AS songName, 
    s.artist_id AS artistId, 
    s.album, 
    s.lyric, 
    s.duration, 
    s.style, 
    s.cover_url AS coverUrl, 
    s.audio_url AS audioUrl, 
    s.release_time AS releaseTime, 
    COALESCE(a.name, u.username) AS artistName,
    s.audit_status AS auditStatus
FROM tb_song s
LEFT JOIN tb_artist a ON s.artist_id = a.id
LEFT JOIN tb_user u ON s.creator_id = u.id
WHERE 
    (NULL IS NULL OR s.artist_id = NULL)
    AND (NULL IS NULL OR s.name LIKE CONCAT('%', NULL, '%'))
    AND (NULL IS NULL OR s.album LIKE CONCAT('%', NULL, '%'))
    AND (s.audit_status IS NULL OR s.audit_status = 1)
ORDER BY s.release_time DESC
LIMIT 20;

-- ============================================
-- 3. 检查歌曲540的详细信息
-- ============================================
SELECT 
    s.id,
    s.name,
    s.audit_status,
    s.is_original,
    s.artist_id,
    s.creator_id,
    s.audio_url,
    s.create_time,
    s.update_time,
    CASE 
        WHEN s.audit_status IS NULL THEN 'NULL（非原创）'
        WHEN s.audit_status = 0 THEN '待审核'
        WHEN s.audit_status = 1 THEN '已通过'
        WHEN s.audit_status = 2 THEN '未通过'
        ELSE '未知'
    END AS '审核状态说明'
FROM tb_song s
WHERE s.id = 540;

-- ============================================
-- 4. 统计符合用户端查询条件的歌曲总数
-- ============================================
SELECT COUNT(*) AS '符合查询条件的歌曲总数'
FROM tb_song s
WHERE (s.audit_status IS NULL OR s.audit_status = 1);

-- ============================================
-- 5. 查看歌曲540在所有符合条件歌曲中的位置
-- ============================================
SELECT 
    COUNT(*) + 1 AS '歌曲540的排名位置'
FROM tb_song s
WHERE (s.audit_status IS NULL OR s.audit_status = 1)
    AND s.id > 540
ORDER BY s.id DESC;

