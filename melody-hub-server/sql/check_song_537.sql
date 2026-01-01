-- 检查歌曲537的数据库数据
SELECT 
    id,
    name,
    artist_id,
    creator_id,
    is_original,
    is_reward_enabled,
    reward_qr_url,
    album,
    cover_url,
    audio_url
FROM tb_song 
WHERE id = 537;

-- 检查创建者信息
SELECT 
    u.id,
    u.username
FROM tb_song s
LEFT JOIN tb_user u ON s.creator_id = u.id
WHERE s.id = 537;

-- 检查艺术家信息
SELECT 
    a.id,
    a.name
FROM tb_song s
LEFT JOIN tb_artist a ON s.artist_id = a.id
WHERE s.id = 537;

