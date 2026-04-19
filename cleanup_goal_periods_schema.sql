-- Clean up GoalPeriod table schema
-- Remove legacy columns and keep only the proper goal_id foreign key

-- 1. Remove the legacy user_id column (not used anymore)
ALTER TABLE goal_periods DROP COLUMN IF EXISTS user_id;

-- 2. Remove the duplicated parent_goal_id column (we use goal_id as foreign key)
ALTER TABLE goal_periods DROP COLUMN IF EXISTS parent_goal_id;

-- 3. Verify the final schema
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'goal_periods' 
  AND table_schema = 'public'
ORDER BY ordinal_position;
