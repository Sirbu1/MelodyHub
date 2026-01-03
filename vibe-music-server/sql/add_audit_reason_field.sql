-- 添加审核备注字段到相关表
-- 执行时间：2025年

-- 为歌曲表添加审核备注字段
ALTER TABLE `tb_song`
ADD COLUMN `audit_reason` varchar(500) NULL DEFAULT NULL COMMENT '审核未通过原因' AFTER `audit_status`;

-- 为论坛帖子表添加审核备注字段
ALTER TABLE `tb_forum_post`
ADD COLUMN `audit_reason` varchar(500) NULL DEFAULT NULL COMMENT '审核未通过原因' AFTER `audit_status`;

-- 为论坛回复表添加审核备注字段
ALTER TABLE `tb_forum_reply`
ADD COLUMN `audit_reason` varchar(500) NULL DEFAULT NULL COMMENT '审核未通过原因' AFTER `audit_status`;

-- 为评论表添加审核备注字段
ALTER TABLE `tb_comment`
ADD COLUMN `audit_reason` varchar(500) NULL DEFAULT NULL COMMENT '审核未通过原因' AFTER `audit_status`;

