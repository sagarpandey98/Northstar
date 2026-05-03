-- Drop metric column from goal_periods
-- Metric is now inherited from parent Goal entity
-- This aligns with the Goal Period Revamp where redundant fields are removed

ALTER TABLE goal_periods DROP COLUMN IF EXISTS metric;

-- Verify the schema after removal
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'goal_periods' 
  AND table_schema = 'public'
ORDER BY ordinal_position;
