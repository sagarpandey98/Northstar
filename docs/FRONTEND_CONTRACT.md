# Frontend Contract

This document defines the contract between the Northstar backend and Node.js frontend, clearly separating responsibilities.

## What Backend Always Returns

The backend computes and returns these fields in GoalResponse:

### Health-Related Fields (Computed by Backend)
- `healthScore` - Overall health score (0-100)
- `consistencyScore` - Consistency component score
- `momentumScore` - Momentum component score  
- `healthStatus` - Status based on health score (THRIVING, ON_TRACK, AT_RISK, CRITICAL, UNTRACKED)
- `currentStreak` - Current streak count
- `longestStreak` - Historical best streak
- `periodConsistencyScore` - Period-based consistency (Phase 9)

### Goal Structure Fields (Computed by Backend)
- `isLeaf` - Whether goal has children (computed at query time)
- `isTracked` - Whether goal has tracking configuration
- `effectiveConsistencyWeight` - Resolved consistency weight
- `effectiveMomentumWeight` - Resolved momentum weight
- `effectiveProgressWeight` - Resolved progress weight

### Parent Goal Fields (Computed by Backend)
- `parentInsights` - Complete parent insights block for parent goals
- `healthScoreLastWeek` - Previous week's health score (for trend calculation)

### Current Period Fields (Computed by Backend)
- `currentPeriodStart` - Start of current evaluation period
- `currentPeriodCount` - Activities in current period

## What Frontend Should Compute

### Health Status Label from Health Score
```javascript
function getHealthStatusLabel(healthScore) {
  if (healthScore === null) return 'UNTRACKED';
  if (healthScore >= 80) return 'THRIVING';
  if (healthScore >= 60) return 'ON_TRACK';
  if (healthScore >= 40) return 'AT_RISK';
  return 'CRITICAL';
}
```

### Health Trend from Health Score Comparison
```javascript
function getHealthTrend(currentScore, lastWeekScore) {
  if (lastWeekScore === null) return 'NEW';
  const diff = currentScore - lastWeekScore;
  if (diff > 3) return 'IMPROVING';
  if (diff < -3) return 'DECLINING';
  return 'STABLE';
}
```

### Color Coding Logic
```javascript
function getHealthColor(healthStatus) {
  switch (healthStatus) {
    case 'THRIVING': return '#10B981'; // Green
    case 'ON_TRACK': return '#3B82F6';  // Blue
    case 'AT_RISK': return '#F59E0B';   // Yellow
    case 'CRITICAL': return '#EF4444';  // Red
    case 'UNTRACKED': return '#6B7280'; // Gray
    default: return '#6B7280';
  }
}
```

### Progress Color Based on Health Score
```javascript
function getProgressColor(healthScore) {
  if (healthScore >= 80) return '#10B981';
  if (healthScore >= 60) return '#3B82F6';
  if (healthScore >= 40) return '#F59E0B';
  return '#EF4444';
}
```

### Regularity Score Suggestion (Optional Enhancement)
```javascript
function suggestRegularityScore(scheduleType, scheduleDays) {
  if (scheduleType === 'SPECIFIC_DAYS') {
    return scheduleDays.length; // e.g., 3x/week for MON,WED,FRI
  }
  return 7; // Default to daily for FLEXIBLE
}
```

## Key Flags

### isLeaf: true/false - Rendering Decisions

**isLeaf: true (Trackable Goals)**
- Show tracking fields: targetFrequencyWeekly, targetPerPeriod
- Display health scores and streak information
- Show progress bars and completion percentage
- Allow activity logging directly
- Show period-based progress if evaluationPeriod is set

**isLeaf: false (Parent Goals)**
- Show parentInsights block instead of individual health
- Display child goal summary and distribution
- Show weakestChild highlight
- Hide tracking-specific fields (targets, streaks)
- Show completionVelocity and trend information

### isTracked: true/false - Tracking Availability

**isTracked: true**
- Show "Track Progress" section
- Display health scores and streaks
- Enable activity logging for this goal
- Show consistency and momentum breakdowns

**isTracked: false**
- Show "Set up tracking" prompt
- Hide health scores (show as UNTRACKED)
- Disable activity logging for this goal
- Display setup guidance

### evaluationPeriod - Period Progress Display

**evaluationPeriod: null or WEEKLY**
- Show weekly progress: "3/5 this week"
- Display weekly target and current count
- Use weekly streak information

**evaluationPeriod: DAILY**
- Show daily progress: "1/2 today"
- Display daily target and current count
- Show daily streak if tracked

**evaluationPeriod: MONTHLY**
- Show monthly progress: "8/4 this month" (exceeding target)
- Display monthly target and current count
- Show month/year in progress display

**evaluationPeriod: QUARTERLY**
- Show quarterly progress: "20/90 Q1"
- Display quarter and year
- Show quarterly target and current count

**evaluationPeriod: YEARLY**
- Show yearly progress: "45/12 this year" (exceeding target)
- Display yearly target and current count
- Show year in progress display

**evaluationPeriod: CUSTOM**
- Show custom period progress: "3/5 (10-day period)"
- Display custom period length and current count
- Show period start/end dates if helpful

## Goal Tree Rendering Rules

### Leaf Goals (isLeaf: true)
```jsx
<GoalCard>
  <GoalHeader title={goal.title} status={goal.status} />
  <ProgressBar progress={goal.progressPercentage} />
  <HealthScore score={goal.healthScore} status={goal.healthStatus} />
  <TrackingInfo 
    target={goal.targetFrequencyWeekly} 
    current={goal.currentPeriodCount}
    period={goal.evaluationPeriod}
  />
  <StreakInfo current={goal.currentStreak} longest={goal.longestStreak} />
  <ActivityLogButton goalId={goal.id} />
</GoalCard>
```

### Parent Goals (isLeaf: false)
```jsx
<ParentGoalCard>
  <GoalHeader title={goal.title} status={goal.status} />
  <ParentInsights insights={goal.parentInsights} />
  <HealthTrend 
    current={goal.healthScore} 
    lastWeek={goal.healthScoreLastWeek} 
  />
  <ChildGoalsList children={goal.childGoals} />
</ParentGoalCard>
```

### Handling Null Health Score (Untracked State)
```jsx
if (goal.healthScore === null) {
  return (
    <UntrackedGoalCard>
      <GoalHeader title={goal.title} status={goal.status} />
      <SetupTrackingPrompt goalId={goal.id} />
      <Explanation text="Set up tracking to see health scores and progress" />
    </UntrackedGoalCard>
  );
}
```

## ParentInsights Block

### All Fields and Display

```jsx
<ParentInsights insights={goal.parentInsights}>
  <SummarySection>
    <Metric label="Total Children" value={insights.totalChildren} />
    <Metric label="Thriving" value={insights.thrivingChildren} color="green" />
    <Metric label="On Track" value={insights.onTrackChildren} color="blue" />
    <Metric label="At Risk" value={insights.atRiskChildren} color="yellow" />
    <Metric label="Critical" value={insights.criticalChildren} color="red" />
    <Metric label="Untracked" value={insights.untrackedChildren} color="gray" />
  </SummarySection>
  
  <DistributionBar data={insights.childrenSummary} />
  
  <WeakestChildSection>
    <Label>Weakest Child</Label>
    <ChildGoal 
      name={insights.weakestChild.title}
      healthScore={insights.weakestChild.healthScore}
      uuid={insights.weakestChild.uuid}
    />
  </WeakestChildSection>
  
  <VelocitySection>
    <Metric label="Overall Progress" value={`${insights.completionVelocity.overallProgress}%`} />
    <Metric label="Average Health" value={`${insights.completionVelocity.averageHealth}`} />
    <TrendArrow direction={insights.completionVelocity.trendDirection} />
  </VelocitySection>
  
  <TrendSection>
    <Label>Health Trend</Label>
    <TrendComparison 
      current={goal.healthScore}
      lastWeek={insights.healthScoreLastWeek}
    />
  </TrendSection>
</ParentInsights>
```

### ChildrenSummary → Distribution Bar
```jsx
function DistributionBar({ data }) {
  const total = Object.values(data).reduce((sum, count) => sum + count, 0);
  
  return (
    <div className="distribution-bar">
      {Object.entries(data).map(([status, count]) => (
        <div 
          key={status}
          className={`segment ${status.toLowerCase()}`}
          style={{ width: `${(count / total) * 100}%` }}
          title={`${status}: ${count}`}
        />
      ))}
    </div>
  );
}
```

### WeakestChild → Highlight in UI
```jsx
function WeakestChild({ child }) {
  return (
    <div className="weakest-child">
      <Icon name="warning" color="red" />
      <span>{child.title}</span>
      <HealthBadge score={child.healthScore} />
      <Link to={`/goals/${child.uuid}`}>View Details</Link>
    </div>
  );
}
```

### CompletionVelocity → Progress Indicators
```jsx
function VelocityIndicators({ velocity }) {
  return (
    <div className="velocity-indicators">
      <ProgressBar 
        label="Overall Progress" 
        value={velocity.overallProgress} 
      />
      <HealthGauge 
        label="Average Health" 
        score={velocity.averageHealth} 
      />
      <TrendIndicator direction={velocity.trendDirection} />
    </div>
  );
}
```

### HealthScoreLastWeek → Trend Arrow
```jsx
function HealthTrend({ currentScore, lastWeekScore }) {
  if (lastWeekScore === null) {
    return <span className="trend-new">New Goal</span>;
  }
  
  const diff = currentScore - lastWeekScore;
  let direction, color;
  
  if (diff > 3) {
    direction = 'up';
    color = 'green';
  } else if (diff < -3) {
    direction = 'down';
    color = 'red';
  } else {
    direction = 'stable';
    color = 'gray';
  }
  
  return (
    <div className="health-trend">
      <Icon name={`trend-${direction}`} color={color} />
      <span>{Math.abs(diff).toFixed(1)} points</span>
    </div>
  );
}
```

## Recommended API Call Sequence

### For Loading the Main Dashboard

**Step 1: GET /api/v1/goals/tree**
```javascript
const response = await fetch('/api/v1/goals/tree', {
  headers: { 'Authorization': `Bearer ${token}` }
});
const { data: goals } = await response.json();
```
- Loads complete goal hierarchy
- Includes all health scores and parent insights
- Primary data source for dashboard

**Step 2: GET /api/v1/goals/health-summary**
```javascript
const healthResponse = await fetch('/api/v1/goals/health-summary', {
  headers: { 'Authorization': `Bearer ${token}` }
});
const { data: healthSummary } = await healthResponse.json();
```
- Loads overall health distribution
- Used for dashboard summary widgets
- Quick overview without full goal details

**Step 3: GET /api/v1/goals/statistics**
```javascript
const statsResponse = await fetch('/api/v1/goals/statistics', {
  headers: { 'Authorization': `Bearer ${token}` }
});
const { data: statistics } = await statsResponse.json();
```
- Loads goal completion statistics
- Used for progress widgets and charts
- Includes Phase 8 health statistics

### For Logging an Activity

**Step 1: POST /api/v1/activities**
```javascript
const activityResponse = await fetch('/api/v1/activities', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    name: activityName,
    startTime: new Date().toISOString(),
    duration: duration,
    goalId: selectedGoalId // Optional linking
  })
});
```

**Step 2: Refresh Goal Health from Response**
```javascript
// No extra call needed - health updates automatically
// The activity response includes updated goal information
// Or refresh the specific goal:
const goalResponse = await fetch(`/api/v1/goals/${selectedGoalId}`, {
  headers: { 'Authorization': `Bearer ${token}` }
});
const updatedGoal = await goalResponse.json();
```

**Key Point**: Health recalculation happens automatically on activity creation. No separate health refresh call is needed unless you want to ensure the latest data.

### For Manual Health Refresh (Debugging/Admin)

```javascript
// Only use when health seems incorrect
const refreshResponse = await fetch(`/api/v1/goals/${goalId}/health/recalculate`, {
  method: 'PATCH',
  headers: { 'Authorization': `Bearer ${token}` }
});
const refreshedGoal = await refreshResponse.json();
```

## Performance Considerations

### Caching Strategy
- Cache goal tree data for 5 minutes (health scores update frequently)
- Cache health summary for 2 minutes (changes often)
- Cache statistics for 10 minutes (changes less frequently)

### Optimistic Updates
- Update UI immediately after activity creation
- Revalidate with server after successful response
- Handle potential health calculation failures gracefully

### Error Handling
- Always check `success` field in responses
- Handle `UNTRACKED` goals gracefully
- Provide fallback UI when health scores are null
- Show loading states during health recalculation
