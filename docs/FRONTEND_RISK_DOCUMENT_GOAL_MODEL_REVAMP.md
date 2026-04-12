# **FRONTEND RISK DOCUMENT: GOAL MODEL REVAMP**

## **MAJOR ARCHITECTURE CLEANUP - ALL FIELD CHANGES**

---

## **OVERVIEW**

**Date**: 2026-04-12  
**Priority**: HIGH  
**Impact**: MAJOR BREAKING CHANGES  
**Scope**: Complete goal model cleanup for period-aware architecture  

**Summary**: Comprehensive cleanup of goal model to fix period inconsistencies, remove legacy weekly-only fields, and create unified period-aware system.

---

## **ALL FIELD CHANGES SUMMARY**

| Field | Current Issue | Proposed Change | Impact |
|-------|---------------|------------------|---------|
| `metric` | Complex dropdown for 95% same value | Default to COUNT, hide options | Low |
| `target_operator` | Complex dropdown for 95% same value | Default to >=, hide options | Low |
| `schedule_type` | Confusing name, weekly-only enum | Rename to `evaluation_period_type`, SPECIFIC_DAYS -> SPECIFIC | Medium |
| `target_frequency_weekly` | Weekly-only, conflicts with other periods | COMPLETE REMOVAL, use `target_per_period` | High |
| `schedule_days` | Weekly-only, conflicts with other periods | Period-aware scheduling system | High |
| `misses_allowed_per_week` | Weekly-only, conflicts with other periods | Period-aware grace periods | High |
| `target_volume_daily` | Daily-only, conflicts with other periods | Period-aware volume targets | High |
| `momentum_score` | Weekly calculation only | Period-aware calculation | High |
| `current_streak` | Weekly calculation only | Period-aware calculation | High |

---

## **PHASE 1: LOW IMPACT CHANGES**

### **1. metric & target_operator Simplification**

**Change**: Default to COUNT + >= for 95% of goals  
**Frontend Impact**: Remove dropdowns, add advanced options toggle  

```json
// BEFORE
{
  "metric": "COUNT",
  "targetOperator": "GREATER_THAN_OR_EQUAL"
}

// AFTER (defaults)
{
  // metric and targetOperator set to COUNT + >= automatically
}
```

**UI Changes**:
- Hide metric selection by default
- Hide target operator by default  
- Add "Advanced Options" toggle for edge cases
- Update validation messages

**Timeline**: 1 week  
**Risk**: LOW  

---

## **PHASE 2: MEDIUM IMPACT CHANGES**

### **2. schedule_type Field Rename**

**Change**: `schedule_type` -> `evaluation_period_type`  
**Enum Update**: `SPECIFIC_DAYS` -> `SPECIFIC`  

```java
// BEFORE
public enum ScheduleType {
    FLEXIBLE, SPECIFIC_DAYS
}

// AFTER  
public enum EvaluationPeriodType {
    FLEXIBLE, SPECIFIC
}
```

**Frontend Impact**:
- Update all form field names
- Update dropdown options
- Update API request/response handling
- Update validation messages

**Timeline**: 2 weeks  
**Risk**: MEDIUM  

---

## **PHASE 3: HIGH IMPACT CHANGES**

### **3. target_frequency_weekly Removal**

**Change**: Complete removal of `target_frequency_weekly` field  
**Replacement**: Use `target_per_period` for all periods  

```json
// BEFORE (confusing)
{
  "evaluationPeriod": "MONTHLY",
  "targetFrequencyWeekly": 5,        // Wrong for monthly
  "targetPerPeriod": 20              // Correct
}

// AFTER (clean)
{
  "evaluationPeriod": "MONTHLY", 
  "targetPerPeriod": 20              // Only this matters
}
```

**Frontend Impact**:
- Remove field from all forms
- Update validation logic
- Update API calls
- Database migration required

**Timeline**: 3 weeks  
**Risk**: HIGH  

---

## **PHASE 4: ARCHITECTURAL CHANGES**

### **4. Period-Aware Systems**

**Fields Needing Period-Aware Logic**:
- `schedule_days` -> Period-specific scheduling
- `misses_allowed_per_week` -> `misses_allowed_per_period` (✅ FIXED: Now period-aware)
- `target_volume_daily` -> `target_volume_per_period`
- `momentum_score` -> Period-aware calculation (✅ FIXED: Now period-agnostic)
- `current_streak` -> Period-aware calculation (✅ FIXED: Now period-agnostic)

### **5. Priority System Enhancement (NEW)**
**Fields Added**:
- **PriorityEngine Service**: New service layer for priority calculations
- **Weighted Priority Logic**: Parent (30%) + Child (70%) priority combination
- **Numerical Priority Scores**: CRITICAL=4, HIGH=3, MEDIUM=2, LOW=1
- **Today's Focus List**: Top N goals across all hierarchies
- **Priority-wise Listings**: Both flattened and grouped by priority

**New API Endpoints**:
- `/api/v1/priority/goals/sorted` - Goals sorted by effective priority
- `/api/v1/priority/goals/grouped` - Goals grouped by priority level
- `/api/v1/priority/goals/today-focus` - Today's Focus recommendations
- `/api/v1/priority/goals/{uuid}/score` - Priority calculation details
- `/api/v1/priority/statistics` - Priority analytics

**Frontend Impact**:
- New Today's Focus component
- Priority score display with parent context
- Cross-hierarchical goal comparison
- Priority analytics dashboard
- Enhanced goal sorting and filtering

**MBA-Style Priority Example**:
```
MBA Goal (CRITICAL=4) + Interview Goal (LOW=1)
= (1 × 0.7) + (4 × 0.3) + 0.5 bonus
= 2.4 (higher than standalone LOW=1.0)
```

**Status**: NEW FEATURE - Ready for implementation

**New Period-Aware Structure**:
```java
// For WEEKLY goals
evaluationPeriod: WEEKLY
targetPerPeriod: 5
scheduleDays: ["MON", "WED", "FRI"]
missesAllowedPerPeriod: 1

// For MONTHLY goals  
evaluationPeriod: MONTHLY
targetPerPeriod: 20
scheduleDays: ["1", "15", "20"]  // Dates of month
missesAllowedPerPeriod: 2

// For DAILY goals
evaluationPeriod: DAILY  
targetPerPeriod: 1
scheduleDays: null
missesAllowedPerPeriod: 0
```

**Frontend Impact**:
- Complete form redesign for period-specific options
- Complex validation logic
- Multiple UI states for different periods
- Extensive testing required

**Timeline**: 4 weeks  
**Risk**: HIGH  

---

## **FRONTEND IMPACT ANALYSIS**

### **Forms Requiring Updates**:
- [ ] Goal Creation Form
- [ ] Goal Editing Form  
- [ ] Goal Settings Modal
- [ ] Bulk Goal Operations
- [ ] Goal Templates

### **API Changes Required**:
- [ ] GoalRequest DTO updates
- [ ] GoalResponse DTO updates
- [ ] Validation logic updates
- [ ] Error message updates
- [ ] API documentation updates

### **UI Components Affected**:
- [ ] Dropdown menus
- [ ] Form validation
- [ ] Help text and tooltips
- [ ] Error messages
- [ ] Progress displays

---

## **DATABASE MIGRATION PLAN**

### **Phase 1: Column Updates**
```sql
-- Rename schedule_type column
ALTER TABLE goals RENAME COLUMN schedule_type TO evaluation_period_type;

-- Remove target_frequency_weekly
ALTER TABLE goals DROP COLUMN target_frequency_weekly;
```

### **Phase 2: Data Migration**
```sql
-- Convert existing weekly targets to period targets
UPDATE goals SET target_per_period = target_frequency_weekly 
WHERE evaluation_period IS NULL OR evaluation_period = 'WEEKLY';

-- Update enum values
UPDATE goals SET evaluation_period_type = 'SPECIFIC' 
WHERE evaluation_period_type = 'SPECIFIC_DAYS';
```

### **Phase 3: New Period-Aware Fields**
```sql
-- Add period-specific fields
ALTER TABLE goals ADD COLUMN misses_allowed_per_period INTEGER;
ALTER TABLE goals ADD COLUMN target_volume_per_period INTEGER;
ALTER TABLE goals ADD COLUMN period_schedule_days VARCHAR(100);
```

---

## **IMPLEMENTATION TIMELINE**

### **Week 1-2: Low Impact Changes**
- metric & target_operator simplification
- Advanced options toggle implementation
- Basic testing and validation

### **Week 3-4: Medium Impact Changes**  
- schedule_type field rename
- Enum value updates
- Form field name updates
- API compatibility layer

### **Week 5-6: High Impact Changes**
- target_frequency_weekly removal
- Database migration
- API updates
- Comprehensive testing

### **Week 7-8: Architectural Changes**
- Period-aware systems implementation
- Complete form redesign
- Advanced validation logic
- Final testing and deployment

---

## **RISK MITIGATION STRATEGIES**

### **Backward Compatibility**
```java
// Support both field names during transition
if (request.getScheduleType() != null) {
    // Old field name - convert
    goal.setEvaluationPeriodType(convertFromScheduleType(request.getScheduleType()));
} else if (request.getEvaluationPeriodType() != null) {
    // New field name - use directly
    goal.setEvaluationPeriodType(request.getEvaluationPeriodType());
}
```

### **Gradual Rollout**
- Feature flags for each phase
- A/B testing for UI changes
- Rollback plans for each phase
- Extensive monitoring

### **Data Safety**
- Database backups before migration
- Migration script testing
- Data validation after migration
- Rollback procedures

---

## **FRONTEND TEAM COMMUNICATION PLAN**

### **Phase 1 Communication**
**Subject**: Goal Model Simplification - metric & target_operator

**Key Points**:
- Simplifying goal creation for 95% of users
- Hiding complex options by default
- Adding advanced options toggle
- Timeline: 1 week
- Low risk change

---

### **Phase 2 Communication**
**Subject**: Field Rename - schedule_type -> evaluation_period_type

**Key Points**:
- Field name change for clarity
- Enum value update (SPECIFIC_DAYS -> SPECIFIC)
- API updates required
- Timeline: 2 weeks  
- Medium risk change

---

### **Phase 3 Communication**
**Subject**: Major Change - target_frequency_weekly Removal

**Key Points**:
- Removing confusing weekly-only field
- Using period-aware targets instead
- Database migration required
- Timeline: 3 weeks
- High risk change

---

### **Phase 4 Communication**
**Subject**: Architecture Update - Period-Aware Systems

**Key Points**:
- Complete period-aware implementation
- Multiple form redesigns
- Complex validation logic
- Timeline: 4 weeks
- High risk change

---

## **TESTING REQUIREMENTS**

### **Frontend Testing**
- [ ] All goal creation flows
- [ ] All goal editing flows  
- [ ] Form validation for each period type
- [ ] API request/response handling
- [ ] Error message display
- [ ] Advanced options toggle
- [ ] Backward compatibility

### **Backend Testing**
- [ ] Database migration scripts
- [ ] Period-aware calculations
- [ ] API compatibility layer
- [ ] Data validation
- [ ] Performance testing

### **Integration Testing**
- [ ] End-to-end goal lifecycle
- [ ] Cross-period functionality
- [ ] Existing goal compatibility
- [ ] Bulk operations
- [ ] Search and filtering

---

## **SUCCESS METRICS**

### **Before Changes**
- Confusing dual target system
- Weekly-only fields with multi-period goals
- Complex goal creation process
- User errors from field conflicts
- Inconsistent behavior across periods

### **After Changes**
- Clean period-aware architecture
- Single target system
- Simplified goal creation
- Consistent behavior across all periods
- Better user experience
- Reduced support tickets

---

## **ROLLBACK PLAN**

### **Phase Rollback**
- Database migration rollback scripts
- Code rollback procedures
- Frontend feature flags
- API version management

### **Complete Rollback**
- Full database restore
- Code revert to previous version
- Frontend deployment rollback
- Communication plan for users

---

## **MONITORING & ALERTING**

### **Key Metrics to Monitor**
- Goal creation success rate
- Form validation errors
- API response times
- Database migration progress
- User error reports

### **Alert Thresholds**
- Error rate > 5%
- Goal creation failure > 3%
- API response time > 2s
- Database migration failures

---

## **POST-IMPLEMENTATION TASKS**

### **Documentation Updates**
- [ ] API documentation
- [ ] Frontend component docs
- [ ] User guides
- [ ] Developer documentation

### **Training & Support**
- [ ] Frontend team training
- [ ] Support team briefing
- [ ] User communication
- [ ] FAQ updates

---

## **25-LINE SUMMARY**

Major goal model cleanup: Simplifying metric/target_operator defaults, renaming schedule_type to evaluation_period_type, removing target_frequency_weekly, implementing period-aware systems for schedule_days, misses_allowed, target_volume, momentum_score, and current_streak. PLUS: New MBA-style priority system with weighted parent-child calculations (70% child + 30% parent), Today's Focus list, and cross-hierarchical goal comparison. Requires 8-week rollout with database migration and complete form redesign.

---

## **FINAL STATUS**

**Ready for Implementation**: YES  
**Frontend Team Review**: REQUIRED  
**Backend Team Review**: REQUIRED  
**QA Team Planning**: REQUIRED  
**Stakeholder Approval**: PENDING  

**Next Steps**: Schedule implementation planning meeting with all teams
