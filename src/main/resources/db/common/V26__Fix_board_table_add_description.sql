-- Add missing description column to board table and increase content size limit

-- Add description column if not exists
ALTER TABLE board ADD COLUMN IF NOT EXISTS description TEXT NOT NULL DEFAULT '';

-- Change content column to TEXT for larger content support
ALTER TABLE board ALTER COLUMN content TYPE TEXT;

-- Update existing empty descriptions to a default value
UPDATE board SET description = '게시글 설명' WHERE description = '';

-- Add comment for documentation
COMMENT ON COLUMN board.description IS 'Board post description';
COMMENT ON COLUMN board.content IS 'Board post content - supports large text content';
