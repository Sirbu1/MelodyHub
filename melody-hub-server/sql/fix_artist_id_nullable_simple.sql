-- 修改tb_song表，允许artist_id为NULL（原创歌曲可能没有关联艺术家）
-- 执行时间：2025年12月
-- 注意：如果外键约束存在，需要先删除外键，修改字段后再重新添加

-- 步骤1：修改artist_id字段，允许为NULL
ALTER TABLE `tb_song`
MODIFY COLUMN `artist_id` bigint(0) NULL COMMENT '歌手 id';
