-- 修复原创歌曲上传所需的数据库字段
-- 执行时间：2025年12月
-- 说明：原创歌曲可能没有关联艺术家、专辑等信息，需要允许这些字段为NULL

-- 1. 允许artist_id为NULL（原创歌曲没有关联艺术家）
ALTER TABLE `tb_song`
MODIFY COLUMN `artist_id` bigint(0) NULL COMMENT '歌手 id';

-- 2. 允许album为NULL或设置默认值（原创歌曲可能没有专辑）
ALTER TABLE `tb_song`
MODIFY COLUMN `album` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '专辑';

-- 3. 允许lyric为NULL（原创歌曲可能没有歌词）
ALTER TABLE `tb_song`
MODIFY COLUMN `lyric` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '歌词';

-- 4. 允许duration为NULL（原创歌曲可能暂时没有时长信息）
ALTER TABLE `tb_song`
MODIFY COLUMN `duration` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '歌曲时长';
