-- 添加用户积分字段
-- 执行时间：2025年

-- 为用户表添加积分字段
ALTER TABLE `tb_user`
ADD COLUMN `score` int(11) NOT NULL DEFAULT 100 COMMENT '用户积分，初始值为100' AFTER `area`;

-- 为现有用户设置初始积分（如果还没有积分）
UPDATE `tb_user` SET `score` = 100 WHERE `score` IS NULL OR `score` = 0;

