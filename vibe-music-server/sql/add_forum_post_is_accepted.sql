-- 为论坛帖子表添加接单状态字段
-- 用于标记需求帖子是否已接单
-- 执行日期：请在执行前填写日期

-- 添加接单状态字段
ALTER TABLE `tb_forum_post`
ADD COLUMN `is_accepted` tinyint DEFAULT 0 COMMENT '是否已接单：0-未接单，1-已接单' AFTER `reference_attachment`;

-- 为现有数据设置默认值为未接单（0）
UPDATE `tb_forum_post` SET `is_accepted` = 0 WHERE `is_accepted` IS NULL;

-- 添加索引以提高查询性能
ALTER TABLE `tb_forum_post`
ADD INDEX `idx_is_accepted`(`is_accepted`) USING BTREE;
