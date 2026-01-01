-- 修改tb_song表，允许album字段为NULL（原创歌曲可能没有专辑）
-- 执行时间：2025年12月

-- 修改album字段，允许为NULL
ALTER TABLE `tb_song`
MODIFY COLUMN `album` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '专辑';