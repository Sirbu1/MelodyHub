-- 清理重复的收藏记录
-- 对于每个用户，每个歌曲只保留一条最新的收藏记录

-- 1. 查看重复的收藏记录（用于检查）
-- SELECT user_id, song_id, type, COUNT(*) as count
-- FROM tb_user_favorite
-- WHERE type = 0 AND song_id IS NOT NULL
-- GROUP BY user_id, song_id, type
-- HAVING COUNT(*) > 1;

-- 2. 删除重复的收藏记录，只保留每个用户每个歌曲的最新一条记录
DELETE t1 FROM tb_user_favorite t1
INNER JOIN tb_user_favorite t2
WHERE t1.id < t2.id
  AND t1.user_id = t2.user_id
  AND t1.type = 0
  AND t1.song_id = t2.song_id
  AND t1.song_id IS NOT NULL;

-- 3. 同样处理歌单的重复收藏
DELETE t1 FROM tb_user_favorite t1
INNER JOIN tb_user_favorite t2
WHERE t1.id < t2.id
  AND t1.user_id = t2.user_id
  AND t1.type = 1
  AND t1.playlist_id = t2.playlist_id
  AND t1.playlist_id IS NOT NULL;

-- 4. 添加唯一索引，防止未来插入重复数据
-- 注意：执行前请先检查索引是否存在，如果已存在会报错，需要先删除

-- 检查索引是否存在（手动执行查看）
-- SHOW INDEX FROM tb_user_favorite WHERE Key_name = 'uk_user_song';
-- SHOW INDEX FROM tb_user_favorite WHERE Key_name = 'uk_user_playlist';

-- 如果索引已存在，需要先删除（取消注释下面的语句）
-- ALTER TABLE tb_user_favorite DROP INDEX `uk_user_song`;
-- ALTER TABLE tb_user_favorite DROP INDEX `uk_user_playlist`;

-- 为歌曲收藏添加唯一索引
-- MySQL 的唯一索引允许多个 NULL 值，所以这个索引对 type=0 且 song_id 不为 NULL 的记录有效
-- 如果索引已存在，此语句会报错，需要先删除旧索引
ALTER TABLE tb_user_favorite
ADD UNIQUE INDEX `uk_user_song` (`user_id`, `type`, `song_id`);

-- 为歌单收藏添加唯一索引
-- 这个索引对 type=1 且 playlist_id 不为 NULL 的记录有效
ALTER TABLE tb_user_favorite
ADD UNIQUE INDEX `uk_user_playlist` (`user_id`, `type`, `playlist_id`);

-- 验证索引创建成功
-- SHOW INDEX FROM tb_user_favorite;

