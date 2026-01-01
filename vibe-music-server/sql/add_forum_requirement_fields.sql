-- 为论坛帖子表添加需求相关字段
-- 用于需求模块的结构化模板

ALTER TABLE `tb_forum_post`
ADD COLUMN `requirement_type` varchar(50) NULL COMMENT '需求类型（如：作曲、编曲、混音、作词等）' AFTER `type`,
ADD COLUMN `time_requirement` varchar(100) NULL COMMENT '时间要求' AFTER `requirement_type`,
ADD COLUMN `budget` varchar(50) NULL COMMENT '预算' AFTER `time_requirement`,
ADD COLUMN `style_description` text NULL COMMENT '风格描述' AFTER `budget`,
ADD COLUMN `reference_attachment` varchar(500) NULL COMMENT '参考附件URL' AFTER `style_description`;

-- 添加索引
ALTER TABLE `tb_forum_post`
ADD INDEX `idx_requirement_type`(`requirement_type`) USING BTREE;

