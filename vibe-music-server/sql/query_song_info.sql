-- 查询歌曲信息的SQL脚本
-- 歌曲文件存放在MinIO中，数据库存储文件URL

-- ============================================
-- 1. 查询歌曲基本信息（包含MinIO文件URL）
-- ============================================

-- 1.1 根据歌曲ID查询单首歌曲
SELECT 
    s.id AS '歌曲ID',
    s.name AS '歌曲名称',
    s.album AS '专辑',
    s.duration AS '时长',
    s.style AS '风格',
    s.cover_url AS '封面URL（MinIO）',
    s.audio_url AS '音频URL（MinIO）',
    s.release_time AS '发行时间',
    s.is_original AS '是否原创',
    s.audit_status AS '审核状态',
    s.creator_id AS '创建者ID',
    a.name AS '艺术家名称',
    u.username AS '创建者名称',
    s.create_time AS '创建时间'
FROM tb_song s
LEFT JOIN tb_artist a ON s.artist_id = a.id
LEFT JOIN tb_user u ON s.creator_id = u.id
WHERE s.id = 歌曲ID;

-- 1.2 查询所有审核通过的歌曲（包含MinIO URL）
SELECT 
    s.id AS '歌曲ID',
    s.name AS '歌曲名称',
    COALESCE(a.name, u.username) AS '艺术家/创建者',
    s.audio_url AS '音频URL（MinIO）',
    s.cover_url AS '封面URL（MinIO）',
    s.audit_status AS '审核状态'
FROM tb_song s
LEFT JOIN tb_artist a ON s.artist_id = a.id
LEFT JOIN tb_user u ON s.creator_id = u.id
WHERE (s.audit_status IS NULL OR s.audit_status = 1)
ORDER BY s.create_time DESC
LIMIT 20;

-- 1.3 查询原创歌曲及其MinIO文件信息
SELECT 
    s.id AS '歌曲ID',
    s.name AS '歌曲名称',
    u.username AS '创建者',
    s.audio_url AS '音频文件URL（MinIO）',
    s.cover_url AS '封面文件URL（MinIO）',
    s.reward_qr_url AS '收款码URL（MinIO，如有）',
    s.audit_status AS '审核状态',
    s.create_time AS '上传时间'
FROM tb_song s
LEFT JOIN tb_user u ON s.creator_id = u.id
WHERE s.is_original = true
ORDER BY s.create_time DESC;

-- ============================================
-- 2. 根据MinIO URL查询歌曲信息
-- ============================================

-- 2.1 根据音频URL查询歌曲
SELECT 
    s.id AS '歌曲ID',
    s.name AS '歌曲名称',
    s.audio_url AS '音频URL',
    s.cover_url AS '封面URL'
FROM tb_song s
WHERE s.audio_url LIKE '%MinIO路径%';

-- 2.2 查询所有MinIO中的音频文件
SELECT 
    s.id AS '歌曲ID',
    s.name AS '歌曲名称',
    s.audio_url AS '音频URL（MinIO）',
    SUBSTRING_INDEX(s.audio_url, '/', -1) AS 'MinIO文件名'
FROM tb_song s
WHERE s.audio_url IS NOT NULL AND s.audio_url != '';

-- ============================================
-- 3. 统计和汇总查询
-- ============================================

-- 3.1 统计MinIO中存储的歌曲数量
SELECT 
    COUNT(*) AS '总歌曲数',
    SUM(CASE WHEN audio_url IS NOT NULL AND audio_url != '' THEN 1 ELSE 0 END) AS '有音频文件的歌曲数',
    SUM(CASE WHEN cover_url IS NOT NULL AND cover_url != '' THEN 1 ELSE 0 END) AS '有封面文件的歌曲数',
    SUM(CASE WHEN is_original = true THEN 1 ELSE 0 END) AS '原创歌曲数',
    SUM(CASE WHEN is_original = true AND audit_status = 1 THEN 1 ELSE 0 END) AS '已审核通过的原创歌曲数'
FROM tb_song;

-- 3.2 查询MinIO文件存储路径分布
SELECT 
    SUBSTRING_INDEX(SUBSTRING_INDEX(audio_url, '/', 4), '/', -1) AS '存储目录',
    COUNT(*) AS '文件数量'
FROM tb_song
WHERE audio_url IS NOT NULL AND audio_url != ''
GROUP BY SUBSTRING_INDEX(SUBSTRING_INDEX(audio_url, '/', 4), '/', -1);

-- ============================================
-- 4. 检查MinIO文件URL的有效性
-- ============================================

-- 4.1 查询音频URL为空的歌曲
SELECT 
    s.id AS '歌曲ID',
    s.name AS '歌曲名称',
    s.audio_url AS '音频URL'
FROM tb_song s
WHERE s.audio_url IS NULL OR s.audio_url = '';

-- 4.2 查询MinIO URL格式不正确的歌曲
SELECT 
    s.id AS '歌曲ID',
    s.name AS '歌曲名称',
    s.audio_url AS '音频URL'
FROM tb_song s
WHERE s.audio_url IS NOT NULL 
  AND s.audio_url != ''
  AND s.audio_url NOT LIKE 'http://%'
  AND s.audio_url NOT LIKE 'https://%';

-- ============================================
-- 5. 根据条件查询歌曲（用于搜索）
-- ============================================

-- 5.1 根据歌曲名称搜索
SELECT 
    s.id AS '歌曲ID',
    s.name AS '歌曲名称',
    COALESCE(a.name, u.username) AS '艺术家/创建者',
    s.audio_url AS '音频URL（MinIO）',
    s.cover_url AS '封面URL（MinIO）'
FROM tb_song s
LEFT JOIN tb_artist a ON s.artist_id = a.id
LEFT JOIN tb_user u ON s.creator_id = u.id
WHERE s.name LIKE '%搜索关键词%'
  AND (s.audit_status IS NULL OR s.audit_status = 1)
LIMIT 20;

-- 5.2 根据创建者搜索原创歌曲
SELECT 
    s.id AS '歌曲ID',
    s.name AS '歌曲名称',
    u.username AS '创建者',
    s.audio_url AS '音频URL（MinIO）',
    s.audit_status AS '审核状态'
FROM tb_song s
LEFT JOIN tb_user u ON s.creator_id = u.id
WHERE u.username LIKE '%用户名%'
  AND s.is_original = true
ORDER BY s.create_time DESC;

