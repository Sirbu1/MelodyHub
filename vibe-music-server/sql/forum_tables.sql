-- 设置字符集
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 论坛帖子表
DROP TABLE IF EXISTS `tb_forum_reply`;
DROP TABLE IF EXISTS `tb_forum_post`;

CREATE TABLE `tb_forum_post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '发帖用户ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '帖子标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '帖子内容',
  `view_count` bigint DEFAULT 0 COMMENT '浏览次数',
  `reply_count` bigint DEFAULT 0 COMMENT '回复数量',
  `like_count` bigint DEFAULT 0 COMMENT '点赞数量',
  `is_top` tinyint DEFAULT 0 COMMENT '是否置顶：0-否，1-是',
  `status` tinyint DEFAULT 0 COMMENT '状态：0-正常，1-已删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛帖子表';

-- 论坛回复表
CREATE TABLE `tb_forum_reply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '回复ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '回复用户ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '回复内容',
  `parent_id` bigint DEFAULT NULL COMMENT '父回复ID（用于楼中楼）',
  `like_count` bigint DEFAULT 0 COMMENT '点赞数量',
  `status` tinyint DEFAULT 0 COMMENT '状态：0-正常，1-已删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛回复表';

-- 插入测试数据
INSERT INTO `tb_forum_post` (`user_id`, `title`, `content`, `view_count`, `reply_count`, `like_count`) VALUES
(1, 'Welcome to Melody Hub Forum', 'This is our music community, welcome to share and discuss music here!', 100, 2, 10),
(1, 'Share your favorite songs', 'What songs do you like? Share them here!', 50, 1, 5);

INSERT INTO `tb_forum_reply` (`post_id`, `user_id`, `content`) VALUES
(1, 1, 'Welcome everyone!'),
(1, 1, 'Have fun here!'),
(2, 1, 'Great topic!');
