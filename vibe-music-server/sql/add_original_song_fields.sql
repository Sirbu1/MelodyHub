-- 添加原创歌曲相关字段到tb_song表
-- 执行时间：2025年12月

ALTER TABLE `tb_song`
ADD COLUMN `creator_id` bigint(0) NULL COMMENT '创建者用户ID（原创歌曲）' AFTER `release_time`,
ADD COLUMN `is_original` tinyint(1) NULL DEFAULT 0 COMMENT '是否原创' AFTER `creator_id`,
ADD COLUMN `is_reward_enabled` tinyint(1) NULL DEFAULT 0 COMMENT '是否开启打赏' AFTER `is_original`,
ADD COLUMN `reward_qr_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '收款码图片URL' AFTER `is_reward_enabled`,
ADD COLUMN `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER `reward_qr_url`,
ADD COLUMN `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`;

-- 添加索引
ALTER TABLE `tb_song`
ADD INDEX `idx_creator_id`(`creator_id`) USING BTREE,
ADD INDEX `idx_is_original`(`is_original`) USING BTREE,
ADD INDEX `idx_create_time`(`create_time`) USING BTREE;

-- 添加外键约束（如果需要的话，可以取消注释）
-- ALTER TABLE `tb_song`
-- ADD CONSTRAINT `fk_song_creator_id` FOREIGN KEY (`creator_id`) REFERENCES `tb_user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;
