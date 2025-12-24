-- 修改tb_song表，允许artist_id为NULL（原创歌曲可能没有关联艺术家）
-- 执行时间：2025年12月

-- 先删除外键约束（如果存在，如果不存在会报错但可以忽略）
-- 注意：如果外键不存在，这个命令会报错，可以忽略
ALTER TABLE `tb_song` 
DROP FOREIGN KEY `fk_song_artist_id`;

-- 修改artist_id字段，允许为NULL
ALTER TABLE `tb_song`
MODIFY COLUMN `artist_id` bigint(0) NULL COMMENT '歌手 id';

-- 重新添加外键约束（允许artist_id为NULL）
ALTER TABLE `tb_song`
ADD CONSTRAINT `fk_song_artist_id` FOREIGN KEY (`artist_id`) REFERENCES `tb_artist` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;
