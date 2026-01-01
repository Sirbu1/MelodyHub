-- 为用户表添加歌手相关字段
ALTER TABLE `tb_user` 
ADD COLUMN `gender` int(0) NULL DEFAULT NULL COMMENT '用户类型：0-男，1-女，2-组合/乐队，3-原创歌手' AFTER `introduction`,
ADD COLUMN `birth` date NULL DEFAULT NULL COMMENT '用户生日' AFTER `gender`,
ADD COLUMN `area` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户国籍' AFTER `birth`;

