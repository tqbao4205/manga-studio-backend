IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('series') AND name = 'is_mature')
    ALTER TABLE series DROP COLUMN is_mature;

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('series') AND name = 'status_note')
    ALTER TABLE series DROP COLUMN status_note;
