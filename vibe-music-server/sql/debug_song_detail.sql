-- 调试：查看歌曲详情SQL查询结果
-- 使用方法：将 537 替换为实际的歌曲ID

SELECT DISTINCT
       s.id           AS songId,
       s.name         AS songName,
       s.album        AS album,
       s.lyric        AS lyric,
       s.duration     AS duration,
       s.cover_url    AS songCoverUrl,
       s.audio_url    AS audioUrl,
       s.release_time AS releaseTime,
       s.is_original  AS isOriginal,
       s.is_reward_enabled AS isRewardEnabled,
       s.reward_qr_url AS rewardQrUrl,
       COALESCE(a.name, creator.username) AS artistName,
       creator.username AS creatorName,
       c.id           AS commentId,
       c.content      AS content,
       c.create_time  AS createTime,
       c.like_count   AS likeCount,
       u.username     AS username,
       u.user_avatar  AS userAvatar
FROM tb_song s
         LEFT JOIN tb_artist a ON s.artist_id = a.id
         LEFT JOIN tb_user creator ON s.creator_id = creator.id
         LEFT JOIN tb_comment c ON s.id = c.song_id AND c.type = 0
         LEFT JOIN tb_user u ON c.user_id = u.id
WHERE s.id = 537
ORDER BY c.create_time DESC;

-- 查看原始歌曲数据
SELECT 
    id,
    name,
    is_original,
    is_reward_enabled,
    reward_qr_url,
    creator_id
FROM tb_song 
WHERE id = 537;

