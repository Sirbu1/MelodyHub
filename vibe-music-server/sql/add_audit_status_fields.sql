-- 添加审核状态字段到相关表
-- 执行时间：2025年

-- 为歌曲表添加审核状态字段
ALTER TABLE `tb_song`
ADD COLUMN `audit_status` tinyint(1) DEFAULT 0 COMMENT '审核状态：0-待审核，1-已通过，2-未通过' AFTER `reward_qr_url`;

-- 为论坛帖子表添加审核状态字段
ALTER TABLE `tb_forum_post`
ADD COLUMN `audit_status` tinyint(1) DEFAULT 0 COMMENT '审核状态：0-待审核，1-已通过，2-未通过' AFTER `reference_attachment`;

-- 为论坛回复表添加审核状态字段
ALTER TABLE `tb_forum_reply`
ADD COLUMN `audit_status` tinyint(1) DEFAULT 0 COMMENT '审核状态：0-待审核，1-已通过，2-未通过' AFTER `status`;

-- 为评论表添加审核状态字段
ALTER TABLE `tb_comment`
ADD COLUMN `audit_status` tinyint(1) DEFAULT 0 COMMENT '审核状态：0-待审核，1-已通过，2-未通过' AFTER `like_count`;

-- 添加索引以提高查询性能
ALTER TABLE `tb_song` ADD INDEX `idx_audit_status` (`audit_status`);
ALTER TABLE `tb_forum_post` ADD INDEX `idx_audit_status` (`audit_status`);
ALTER TABLE `tb_forum_reply` ADD INDEX `idx_audit_status` (`audit_status`);
ALTER TABLE `tb_comment` ADD INDEX `idx_audit_status` (`audit_status`);

-- 将现有数据设置为已通过（1），以便现有内容可以正常显示
UPDATE `tb_song` SET `audit_status` = 1 WHERE `audit_status` IS NULL OR `audit_status` = 0;
UPDATE `tb_forum_post` SET `audit_status` = 1 WHERE `audit_status` IS NULL OR `audit_status` = 0;
UPDATE `tb_forum_reply` SET `audit_status` = 1 WHERE `audit_status` IS NULL OR `audit_status` = 0;
UPDATE `tb_comment` SET `audit_status` = 1 WHERE `audit_status` IS NULL OR `audit_status` = 0;

