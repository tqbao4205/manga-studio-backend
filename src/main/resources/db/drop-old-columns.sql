-- ============================================================
-- Migration: drop old single-value columns, replaced by
-- @ElementCollection join tables (series_genres,
-- series_target_demographics).
-- Chạy sau khi Hibernate đã tạo bảng phụ (ddl-auto=update).
-- ============================================================

-- 1. Drop CHECK constraints trước (nếu có)
DECLARE @sql NVARCHAR(MAX) = '';

SELECT @sql = @sql + 'ALTER TABLE series DROP CONSTRAINT ' + QUOTENAME(c.name) + ';' + CHAR(10)
FROM sys.columns col
JOIN sys.check_constraints c ON c.parent_object_id = col.object_id AND c.parent_column_id = col.column_id
WHERE col.object_id = OBJECT_ID('series')
  AND col.name IN ('genre', 'target_demographic');

EXEC sp_executesql @sql;

-- 2. Drop columns
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('series') AND name = 'genre')
    ALTER TABLE series DROP COLUMN genre;

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('series') AND name = 'target_demographic')
    ALTER TABLE series DROP COLUMN target_demographic;
