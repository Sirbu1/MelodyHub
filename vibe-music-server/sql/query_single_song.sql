-- 查询单条歌曲信息的SQL脚本
-- 使用方法：将下面的 歌曲ID 替换为实际要查询的歌曲ID

-- ============================================
-- 方法1：查询歌曲完整信息（包含MinIO文件URL）
-- ============================================
SELECT 
    s.id AS '歌曲ID',
    s.name AS '歌曲名称',
    s.album AS '专辑',
    s.duration AS '时长',
    s.style AS '风格',
    s.lyric AS '歌词',
    s.cover_url AS '封面URL（MinIO）',
    s.audio_url AS '音频URL（MinIO）',
    s.release_time AS '发行时间',
    s.is_original AS '是否原创',
    s.is_reward_enabled AS '是否开启打赏',
    s.reward_qr_url AS '收款码URL（MinIO）',
    s.audit_status AS '审核状态（0-待审核，1-已通过，2-未通过）',
    s.artist_id AS '艺术家ID',
    s.creator_id AS '创建者ID',
    a.name AS '艺术家名称',
    u.username AS '创建者名称',
    s.create_time AS '创建时间',
    s.update_time AS '更新时间'
FROM tb_song s
LEFT JOIN tb_artist a ON s.artist_id = a.id
LEFT JOIN tb_user u ON s.creator_id = u.id
WHERE s.id = 1;  -- 将 1 替换为要查询的歌曲ID

-- ============================================
-- 方法2：查询歌曲基本信息（简化版）
-- ============================================
SELECT 
    s.id AS '歌曲ID',
    s.name AS '歌曲名称',
    COALESCE(a.name, u.username) AS '艺术家/创建者',
    s.audio_url AS '音频文件URL',
    s.cover_url AS '封面文件URL',
    s.audit_status AS '审核状态',
    CASE s.audit_status
        WHEN 0 THEN '待审核'
        WHEN 1 THEN '已通过'
        WHEN 2 THEN '未通过'
        ELSE '未知'
    END AS '审核状态说明'
FROM tb_song s
LEFT JOIN tb_artist a ON s.artist_id = a.id
LEFT JOIN tb_user u ON s.creator_id = u.id
WHERE s.id = 1;  -- 将 1 替换为要查询的歌曲ID

-- ============================================
-- 方法3：查询歌曲的MinIO文件信息
-- ============================================
SELECT 
    s.id AS '歌曲ID',
    s.name AS '歌曲名称',
    s.audio_url AS '音频URL',
    SUBSTRING_INDEX(s.audio_url, '/', -1) AS '音频文件名',
    s.cover_url AS '封面URL',
    SUBSTRING_INDEX(s.cover_url, '/', -1) AS '封面文件名',
    s.reward_qr_url AS '收款码URL',
    SUBSTRING_INDEX(s.reward_qr_url, '/', -1) AS '收款码文件名'
FROM tb_song s
WHERE s.id = 1;  -- 将 1 替换为要查询的歌曲ID

-- ============================================
-- 方法4：查询歌曲及其关联信息
-- ============================================
SELECT 
    s.id AS '歌曲ID',
    s.name AS '歌曲名称',
    s.album AS '专辑',
    s.duration AS '时长',
    s.style AS '风格',
    s.cover_url AS '封面URL',
    s.audio_url AS '音频URL',
    s.is_original AS '是否原创',
    s.audit_status AS '审核状态',
    -- 艺术家信息
    a.id AS '艺术家ID',
    a.name AS '艺术家名称',
    -- 创建者信息
    u.id AS '创建者ID',
    u.username AS '创建者名称',
    u.user_avatar AS '创建者头像',
    -- 时间信息
    s.release_time AS '发行时间',
    s.create_time AS '创建时间',
    s.update_time AS '更新时间'
FROM tb_song s
LEFT JOIN tb_artist a ON s.artist_id = a.id
LEFT JOIN tb_user u ON s.creator_id = u.id
WHERE s.id = 1;  -- 将 1 替换为要查询的歌曲ID

-- ============================================
-- 方法5：根据歌曲名称查询（如果不知道ID）
-- ============================================
SELECT 
    s.id AS '歌曲ID',
    s.name AS '歌曲名称',
    COALESCE(a.name, u.username) AS '艺术家/创建者',
    s.audio_url AS '音频URL',
    s.cover_url AS '封面URL',
    s.audit_status AS '审核状态'
FROM tb_song s
LEFT JOIN tb_artist a ON s.artist_id = a.id
LEFT JOIN tb_user u ON s.creator_id = u.id
WHERE s.name LIKE '%歌曲名称%'  -- 将 '歌曲名称' 替换为实际歌曲名称
LIMIT 10;

-- ============================================
-- 方法6：查询最近上传的歌曲（用于测试）
-- ============================================
SELECT 
    s.id AS '歌曲ID',
    s.name AS '歌曲名称',
    COALESCE(a.name, u.username) AS '艺术家/创建者',
    s.audio_url AS '音频URL',
    s.audit_status AS '审核状态',
    s.create_time AS '上传时间'
FROM tb_song s
LEFT JOIN tb_artist a ON s.artist_id = a.id
LEFT JOIN tb_user u ON s.creator_id = u.id
ORDER BY s.create_time DESC
LIMIT 5;

