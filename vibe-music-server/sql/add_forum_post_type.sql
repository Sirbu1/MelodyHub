-- 为论坛帖子表添加类型字段
-- 类型：0-交流，1-需求

ALTER TABLE `tb_forum_post`
ADD COLUMN `type` tinyint(1) NOT NULL DEFAULT 0 COMMENT '帖子类型：0-交流，1-需求' AFTER `status`;

-- 为现有数据设置默认类型为交流
UPDATE `tb_forum_post` SET `type` = 0 WHERE `type` IS NULL;

-- 添加索引
ALTER TABLE `tb_forum_post`
ADD INDEX `idx_type`(`type`) USING BTREE;

