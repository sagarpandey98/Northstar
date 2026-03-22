# API Reference

## Goal Endpoints (/api/v1/goals)

### GET /api/v1/goals
- **What it does**: Retrieve all goals for authenticated user
- **Auth required**: Yes
- **Request body**: None
- **Response structure**:
```json
{
  "success": true,
  "message": "Goals retrieved successfully",
  "data": [
    {
      "id": 1,
      "uuid": "goal-uuid",
      "userId": "user-123",
      "title": "Daily Exercise",
      "description": "Exercise for 30 minutes daily",
      "notes": "Focus on cardio",
      "priority": "HIGH",
      "status": "IN_PROGRESS",
      "metric": "DURATION",
      "targetOperator": "GREATER_THAN",
      "targetValue": 1800.0,
      "currentValue": 450.0,
      "progressPercentage": 25.0,
      "startDate": "2024-01-01T00:00:00",
      "targetDate": "2024-12-31T23:59:59",
      "completedDate": null,
      "parentGoalId": null,
      "isMilestone": false,
      "createdAt": "2024-01-01T00:00:00",
      "lastUpdatedAt": "2024-01-15T00:00:00",
      "childGoals": [],
      
      // Phase 2 fields
      "goalType": "FITNESS",
      "isLeaf": true,
      "isTracked": true,
      "targetFrequencyWeekly": 7,
      "targetVolumeDaily": 30,
      "scheduleType": "SPECIFIC_DAYS",
      "scheduleDays": ["MON", "WED", "FRI"],
      "minimumSessionMinutes": 15,
      "allowDoubleLogging": false,
      "missesAllowedPerWeek": 1,
      "missesAllowedPerMonth": 4,
      "effectiveConsistencyWeight": 50,
      "effectiveMomentumWeight": 40,
      "effectiveProgressWeight": 10,
      "consistencyWeight": null,
      "momentumWeight": null,
      "progressWeight": null,
      "consistencyScore": 85.5,
      "momentumScore": 92.0,
      "healthScore": 88.2,
      "healthStatus": "ON_TRACK",
      "currentStreak": 3,
      "longestStreak": 8,
      "parentInsights": null,
      
      // 🆕 Phase 9 fields
      "evaluationPeriod": "WEEKLY",
      "targetPerPeriod": 7,
      "customPeriodDays": null,
      "currentPeriodStart": "2024-01-15",
      "currentPeriodCount": 5,
      "periodConsistencyScore": 71.4
    }
  ]
}
```

### POST /api/v1/goals
- **What it does**: Create a new goal
- **Auth required**: Yes
- **Request body**:
```json
{
  "title": "Daily Exercise",
  "description": "Exercise for 30 minutes daily",
  "notes": "Focus on cardio",
  "priority": "HIGH",
  "status": "NOT_STARTED",
  "metric": "DURATION",
  "targetOperator": "GREATER_THAN",
  "targetValue": 1800.0,
  "currentValue": 0.0,
  "startDate": "2024-01-01T00:00:00",
  "targetDate": "2024-12-31T23:59:59",
  "parentGoalId": null,
  "isMilestone": false,
  
  // Phase 2 fields
  "goalType": "FITNESS",
  "targetFrequencyWeekly": 7,
  "targetVolumeDaily": 30,
  "scheduleType": "SPECIFIC_DAYS",
  "scheduleDays": ["MON", "WED", "FRI"],
  "minimumSessionMinutes": 15,
  "allowDoubleLogging": false,
  "missesAllowedPerWeek": 1,
  "missesAllowedPerMonth": 4,
  "consistencyWeight": null,
  "momentumWeight": null,
  "progressWeight": null,
  
  // 🆕 Phase 9 fields
  "evaluationPeriod": "WEEKLY",
  "targetPerPeriod": 7,
  "customPeriodDays": null
}
```
- **Response structure**: Same as GET /api/v1/goals (single goal in data array)

### GET /api/v1/goals/{id}
- **What it does**: Retrieve specific goal by ID
- **Auth required**: Yes
- **Request body**: None
- **Response structure**: Same as individual goal in GET /api/v1/goals

### PUT /api/v1/goals/{id}
- **What it does**: Update entire goal
- **Auth required**: Yes
- **Request body**: Same as POST /api/v1/goals
- **Response structure**: Same as GET /api/v1/goals/{id}

### PATCH /api/v1/goals/{id}
- **What it does**: Partial update goal (only provided fields)
- **Auth required**: Yes
- **Request body**: Partial goal object (only fields to update)
- **Response structure**: Same as GET /api/v1/goals/{id}

### DELETE /api/v1/goals/{id}
- **What it does**: Soft delete goal (mark as deleted)
- **Auth required**: Yes
- **Request body**: None
- **Response structure**:
```json
{
  "success": true,
  "message": "Goal deleted successfully",
  "data": null
}
```

### GET /api/v1/goals/tree
- **What it does**: Retrieve goals in hierarchical tree structure
- **Auth required**: Yes
- **Request body**: None
- **Response structure**: Same as GET /api/v1/goals but with nested childGoals arrays

### GET /api/v1/goals/statistics
- **What it does**: Retrieve goal statistics for dashboard
- **Auth required**: Yes
- **Request body**: None
- **Response structure**:
```json
{
  "success": true,
  "message": "Goal statistics retrieved successfully",
  "data": {
    "totalGoals": 10,
    "completedGoals": 2,
    "inProgressGoals": 6,
    "notStartedGoals": 2,
    "overdueGoals": 1,
    "dueSoonGoals": 3,
    "milestones": 3,
    "overallCompletionPercentage": 20.0,
    "overduePercentage": 10.0,
    "goalsByPriority": {
      "LOW": 2,
      "MEDIUM": 4,
      "HIGH": 3,
      "CRITICAL": 1
    },
    "goalsByStatus": {
      "NOT_STARTED": 2,
      "IN_PROGRESS": 6,
      "COMPLETED": 2,
      "OVERDUE": 0
    },
    
    // 🆕 Phase 8 health fields
    "thrivingGoals": 3,
    "onTrackGoals": 4,
    "atRiskGoals": 2,
    "criticalGoals": 1,
    "untrackedGoals": 0,
    "averageHealthScore": 72.5,
    "trackedGoals": 8,
    "leafGoals": 7
  }
}
```

### 🆕 GET /api/v1/goals/health-summary
- **What it does**: Retrieve health summary for all goals
- **Auth required**: Yes
- **Request body**: None
- **Response structure**:
```json
{
  "success": true,
  "message": "Health summary retrieved successfully",
  "data": {
    "totalGoals": 10,
    "thrivingGoals": 3,
    "onTrackGoals": 4,
    "atRiskGoals": 2,
    "criticalGoals": 1,
    "untrackedGoals": 0,
    "averageHealthScore": 72.5,
    "healthDistribution": {
      "THRIVING": 3,
      "ON_TRACK": 4,
      "AT_RISK": 2,
      "CRITICAL": 1,
      "UNTRACKED": 0
    }
  }
}
```

### 🆕 PATCH /api/v1/goals/{id}/health/recalculate
- **What it does**: Manually recalculate health score for specific goal
- **Auth required**: Yes
- **Request body**: None
- **Response structure**: Same as GET /api/v1/goals/{id} (with updated health scores)

### GET /api/v1/goals/search
- **What it does**: Search goals by title or description
- **Auth required**: Yes
- **Request body**: None
- **Query parameters**: `q` (search term)
- **Response structure**: Same as GET /api/v1/goals (filtered results)

### GET /api/v1/goals/by-status/{status}
- **What it does**: Filter goals by status
- **Auth required**: Yes
- **Request body**: None
- **Response structure**: Same as GET /api/v1/goals (filtered by status)

### GET /api/v1/goals/by-priority/{priority}
- **What it does**: Filter goals by priority
- **Auth required**: Yes
- **Request body**: None
- **Response structure**: Same as GET /api/v1/goals (filtered by priority)

### GET /api/v1/goals/by-type/{type}
- **What it does**: Filter goals by goal type
- **Auth required**: Yes
- **Request body**: None
- **Response structure**: Same as GET /api/v1/goals (filtered by goal type)

### GET /api/v1/goals/{id}/children
- **What it does**: Retrieve direct children of a goal
- **Auth required**: Yes
- **Request body**: None
- **Response structure**: Same as GET /api/v1/goals (only children)

### GET /api/v1/goals/{id}/tree
- **What it does**: Retrieve subtree starting from specific goal
- **Auth required**: Yes
- **Request body**: None
- **Response structure**: Same as GET /api/v1/goals/tree (subtree only)

### GET /api/v1/goals/leaf
- **What it does**: Retrieve only leaf goals (no children)
- **Auth required**: Yes
- **Request body**: None
- **Response structure**: Same as GET /api/v1/goals (only leaf goals)

### GET /api/v1/goals/parent
- **What it does**: Retrieve only parent goals (have children)
- **Auth required**: Yes
- **Request body**: None
- **Response structure**: Same as GET /api/v1/goals (only parent goals)

## Activity Endpoints (/api/v1/activities)

### GET /api/v1/activities
- **What it does**: Retrieve all activities for authenticated user
- **Auth required**: Yes
- **Request body**: None
- **Response structure**:
```json
{
  "success": true,
  "message": "Activities retrieved successfully",
  "data": [
    {
      "id": 1,
      "uuid": "activity-uuid",
      "userId": "user-123",
      "name": "Morning Run",
      "startTime": "2024-01-15T06:30:00Z",
      "duration": "30 minutes",
      "endTime": "2024-01-15T07:00:00Z",
      "description": "5K run in the park",
      "domainName": "Health",
      "subdomainName": "Fitness",
      "specificName": "Running",
      "domainId": "domain-uuid",
      "subdomainId": "subdomain-uuid",
      "specificId": "specific-uuid",
      "mood": 4,
      "rating": 5,
      "source": "MANUAL",
      "goalId": 1, // 🆕 Optional goal linking
      "createdAt": "2024-01-15T07:05:00",
      "updatedAt": "2024-01-15T07:05:00",
      "isDeleted": false
    }
  ]
}
```

### POST /api/v1/activities
- **What it does**: Create a new activity
- **Auth required**: Yes
- **Request body**:
```json
{
  "name": "Morning Run",
  "startTime": "2024-01-15T06:30:00Z",
  "duration": "30 minutes",
  "endTime": "2024-01-15T07:00:00Z",
  "description": "5K run in the park",
  "domainName": "Health",
  "subdomainName": "Fitness",
  "specificName": "Running",
  "domainId": "domain-uuid",
  "subdomainId": "subdomain-uuid",
  "specificId": "specific-uuid",
  "mood": 4,
  "rating": 5,
  "source": "MANUAL",
  "goalId": 1 // 🆕 Optional goal linking
}
```
- **Response structure**: Same as individual activity in GET /api/v1/activities

### GET /api/v1/activities/{id}
- **What it does**: Retrieve specific activity by ID
- **Auth required**: Yes
- **Request body**: None
- **Response structure**: Same as individual activity in GET /api/v1/activities

### DELETE /api/v1/activities/{id}
- **What it does**: Soft delete activity
- **Auth required**: Yes
- **Request body**: None
- **Response structure**:
```json
{
  "success": true,
  "message": "Activity deleted successfully",
  "data": null
}
```

### GET /api/v1/activities/search
- **What it does**: Search activities by name or description
- **Auth required**: Yes
- **Request body**: None
- **Query parameters**: `q` (search term), `domain`, `subdomain`, `specific`
- **Response structure**: Same as GET /api/v1/activities (filtered results)

## Category Endpoints (/api/v1/categories)

### GET /api/v1/categories/domains
- **What it does**: Retrieve all domains for authenticated user
- **Auth required**: Yes
- **Request body**: None
- **Response structure**:
```json
{
  "success": true,
  "message": "Domains retrieved successfully",
  "data": [
    {
      "id": 1,
      "uuid": "domain-uuid",
      "userId": "user-123",
      "name": "Health",
      "description": "Health and fitness activities",
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00",
      "isDeleted": false,
      "subdomains": [
        {
          "id": 1,
          "uuid": "subdomain-uuid",
          "name": "Fitness",
          "description": "Physical fitness activities",
          "specifics": [
            {
              "id": 1,
              "uuid": "specific-uuid",
              "name": "Running",
              "description": "Running activities"
            }
          ]
        }
      ]
    }
  ]
}
```

### POST /api/v1/categories/domains
- **What it does**: Create a new domain
- **Auth required**: Yes
- **Request body**:
```json
{
  "name": "Health",
  "description": "Health and fitness activities"
}
```
- **Response structure**: Same as individual domain in GET /api/v1/categories/domains

### GET /api/v1/categories/subdomains
- **What it does**: Retrieve all subdomains for authenticated user
- **Auth required**: Yes
- **Request body**: None
- **Response structure**: Same as subdomain objects in domains response

### POST /api/v1/categories/subdomains
- **What it does**: Create a new subdomain
- **Auth required**: Yes
- **Request body**:
```json
{
  "name": "Fitness",
  "description": "Physical fitness activities",
  "domainId": "domain-uuid"
}
```
- **Response structure**: Same as individual subdomain

### GET /api/v1/categories/specifics
- **What it does**: Retrieve all specifics for authenticated user
- **Auth required**: Yes
- **Request body**: None
- **Response structure**: Same as specific objects in domains response

### POST /api/v1/categories/specifics
- **What it does**: Create a new specific
- **Auth required**: Yes
- **Request body**:
```json
{
  "name": "Running",
  "description": "Running activities",
  "subdomainId": "subdomain-uuid"
}
```
- **Response structure**: Same as individual specific

## Error Responses

### Standard Error Format
```json
{
  "success": false,
  "message": "Error description",
  "error": "ERROR_CODE",
  "details": "Additional error details"
}
```

### Common Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| VALIDATION_ERROR | 400 | Request validation failed |
| GOAL_NOT_FOUND | 404 | Goal not found or doesn't belong to user |
| ACTIVITY_NOT_FOUND | 404 | Activity not found or doesn't belong to user |
| CATEGORY_NOT_FOUND | 404 | Category not found or doesn't belong to user |
| UNAUTHORIZED | 401 | Invalid or missing authentication |
| FORBIDDEN | 403 | Access denied to resource |
| DUPLICATE_UUID | 409 | UUID already exists (shouldn't happen with proper generation) |
| PROCESSING_ERROR | 500 | Internal server error during processing |

### Validation Error Details
```json
{
  "success": false,
  "message": "Validation failed",
  "error": "VALIDATION_ERROR",
  "details": {
    "title": "Goal title is required",
    "priority": "Goal priority is required",
    "targetPerPeriod": "targetPerPeriod is required and must be greater than 0 when evaluationPeriod is set"
  }
}
```

## Authentication

### How JWT Works
- **Resource Server**: Northstar Activity Tracker (port 8081)
- **Auth Server**: User Service (port 8082)
- **JWT Claim**: `sub` (subject) contains the user ID
- **Flow**: Frontend gets JWT from auth server, includes in Authorization header

### Authorization Header
```
Authorization: Bearer <jwt_token>
```

### User ID Extraction
The system extracts `userId` from the JWT `sub` claim and uses it for:
- Filtering user-specific data
- Authorization checks
- Activity and goal ownership validation

### Security Configuration
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8082
```

This configures Northstar to validate JWTs issued by the User Service at localhost:8082.
