-- 更新论坛接单表的状态字段注释，添加状态3（已拒绝）
-- 状态：0-待同意，1-已接单未完成，2-已完成，3-已拒绝

ALTER TABLE `tb_forum_order` 
MODIFY COLUMN `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0-待同意，1-已接单未完成，2-已完成，3-已拒绝';

