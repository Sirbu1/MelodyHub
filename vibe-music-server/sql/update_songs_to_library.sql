-- 将已上传的待审核歌曲批量更新为已通过状态，使其出现在曲库中
-- 注意：此脚本会将所有待审核的原创歌曲设置为已通过状态
-- 执行前请确认是否符合业务需求

-- 方法1：将所有待审核的原创歌曲设置为已通过
UPDATE `tb_song`
SET `audit_status` = 1, `update_time` = NOW()
WHERE `is_original` = true 
  AND `audit_status` = 0;

-- 方法2：将指定歌曲ID的审核状态设置为已通过（更安全的方式）
-- UPDATE `tb_song`
-- SET `audit_status` = 1, `update_time` = NOW()
-- WHERE `id` IN (歌曲ID1, 歌曲ID2, 歌曲ID3);

-- 方法3：查看当前待审核的歌曲（执行更新前先查看）
-- SELECT 
--     id AS '歌曲ID',
--     name AS '歌曲名称',
--     creator_id AS '创建者ID',
--     audit_status AS '审核状态',
--     create_time AS '创建时间'
-- FROM `tb_song`
-- WHERE `is_original` = true AND `audit_status` = 0
-- ORDER BY `create_time` DESC;

