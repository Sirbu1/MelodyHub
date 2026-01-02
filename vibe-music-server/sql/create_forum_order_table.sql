-- 创建论坛接单表
-- 用于记录需求帖子的接单信息

CREATE TABLE IF NOT EXISTS `tb_forum_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '接单ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `poster_id` bigint NOT NULL COMMENT '需求发布者ID',
  `accepter_id` bigint NOT NULL COMMENT '接单者ID',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0-待同意，1-已接单未完成，2-已完成',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_poster_id` (`poster_id`),
  KEY `idx_accepter_id` (`accepter_id`),
  KEY `idx_status` (`status`),
  UNIQUE KEY `uk_post_accepter` (`post_id`, `accepter_id`) COMMENT '同一帖子同一用户只能接单一次'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛接单表';

