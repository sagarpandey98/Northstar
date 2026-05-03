-- ============================================================================
-- Goal Period Revamp V1 - Database Schema Cleanup Migration
-- ============================================================================
-- This migration script removes legacy columns from goal_periods that were
-- deprecated during the Goal Period Revamp. These fields should now be
-- accessed from the parent Goal entity instead.
--
-- Migration Steps:
-- 1. Backup your database before running this script
-- 2. Execute this script against your database
-- 3. Verify data integrity with the verification queries
-- 4. Deploy the updated Java application
-- ============================================================================

-- ============================================================================
-- PHASE 1: PRE-MIGRATION VERIFICATION
-- ============================================================================

-- Check current schema before migration
SELECT 
    column_name, 
    data_type, 
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'goal_periods' 
  AND table_schema = 'public'
ORDER BY ordinal_position;

-- Count total records before migration
SELECT COUNT(*) as total_periods FROM goal_periods;

-- Identify any periods with NULL goal_id (data quality check)
SELECT COUNT(*) as null_goal_ids FROM goal_periods WHERE goal_id IS NULL;

-- ============================================================================
-- PHASE 2: REMOVE DEPRECATED COLUMNS
-- ============================================================================

-- Drop columns that now come from parent Goal entity
-- These columns were deprecated in Goal Period Revamp V1

ALTER TABLE goal_periods DROP COLUMN IF EXISTS completed_date CASCADE;
ALTER TABLE goal_periods DROP COLUMN IF EXISTS target_operator CASCADE;
ALTER TABLE goal_periods DROP COLUMN IF EXISTS metric CASCADE;
ALTER TABLE goal_periods DROP COLUMN IF EXISTS consistency_weight CASCADE;
ALTER TABLE goal_periods DROP COLUMN IF EXISTS momentum_weight CASCADE;
ALTER TABLE goal_periods DROP COLUMN IF EXISTS progress_weight CASCADE;
ALTER TABLE goal_periods DROP COLUMN IF EXISTS minimum_session_period CASCADE;
ALTER TABLE goal_periods DROP COLUMN IF EXISTS maximum_session_period CASCADE;
ALTER TABLE goal_periods DROP COLUMN IF EXISTS misses_allowed_per_period CASCADE;
ALTER TABLE goal_periods DROP COLUMN IF EXISTS allow_double_logging CASCADE;

-- ============================================================================
-- PHASE 3: ADD MISSING COLUMNS (if not present)
-- ============================================================================

-- Ensure schedule_spec exists and has proper type
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns 
    WHERE table_name = 'goal_periods' AND column_name = 'schedule_spec'
  ) THEN
    ALTER TABLE goal_periods ADD COLUMN schedule_spec text;
  END IF;
END
$$;

-- ============================================================================
-- PHASE 4: VERIFY SCHEMA INTEGRITY
-- ============================================================================

-- Final schema verification
SELECT 
    column_name, 
    data_type, 
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'goal_periods' 
  AND table_schema = 'public'
ORDER BY ordinal_position;

-- ============================================================================
-- PHASE 5: DATA QUALITY CHECKS
-- ============================================================================

-- Verify no orphaned periods (all should have valid parent goal)
SELECT COUNT(*) as orphaned_periods 
FROM goal_periods gp 
LEFT JOIN goals g ON gp.goal_id = g.uuid
WHERE g.uuid IS NULL;

-- Verify period date ranges are valid
SELECT COUNT(*) as invalid_periods 
FROM goal_periods 
WHERE period_start > period_end;

-- Check for duplicate periods (same goal, same date range)
SELECT goal_id, period_start, period_end, COUNT(*) as duplicate_count
FROM goal_periods 
GROUP BY goal_id, period_start, period_end
HAVING COUNT(*) > 1;

-- ============================================================================
-- PHASE 6: FINAL STATUS
-- ============================================================================

-- Count total records after migration
SELECT COUNT(*) as total_periods FROM goal_periods;

-- Display final schema
\d goal_periods

-- ============================================================================
-- NOTES FOR DEPLOYMENT
-- ============================================================================
-- 
-- 1. Timeline:
--    - Dev: Immediate
--    - Staging: After dev verification
--    - Production: After staging sign-off
--
-- 2. Data Integrity:
--    - All removed columns are now derived from parent Goal
--    - No data loss - only unused columns removed
--    - Backup recommended before running
--
-- 3. Application Deployment:
--    - Deploy Java application AFTER running this migration
--    - Application will not write to removed columns
--    - Reading from removed columns will fail gracefully (nulls)
--
-- 4. Rollback:
--    - If needed, restore from backup or recreate columns from Goal parent
--    - No data recovery needed as info is still in Goal entity
--
-- ============================================================================
