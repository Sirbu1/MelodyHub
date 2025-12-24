-- 完整修复原创歌曲相关字段，允许为NULL
-- 执行时间：2025年12月
-- 说明：原创歌曲可能没有关联艺术家和专辑，这些字段应该允许为NULL

-- 1. 修改artist_id字段，允许为NULL
ALTER TABLE `tb_song`
MODIFY COLUMN `artist_id` bigint(0) NULL COMMENT '歌手 id';

-- 2. 修改album字段，允许为NULL
ALTER TABLE `tb_song`
MODIFY COLUMN `album` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '专辑';

-- 验证修改结果
-- SELECT COLUMN_NAME, IS_NULLABLE, COLUMN_DEFAULT 
-- FROM INFORMATION_SCHEMA.COLUMNS 
-- WHERE TABLE_NAME = 'tb_song' 
-- AND TABLE_SCHEMA = 'vibe_music'
-- AND COLUMN_NAME IN ('artist_id', 'album');
