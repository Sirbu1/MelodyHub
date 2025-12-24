-- 测试数据库迁移是否成功
-- 检查tb_song表是否包含新添加的字段

DESCRIBE tb_song;

-- 检查新字段是否正确添加
SELECT
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_NAME = 'tb_song'
AND TABLE_SCHEMA = 'vibe_music'
ORDER BY ORDINAL_POSITION;
