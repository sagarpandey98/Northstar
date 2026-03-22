# Goal Management API Documentation

## 📋 Overview

The Goal Management API provides a comprehensive system for managing personal and professional goals with **progress tracking**, **hierarchical organization**, and **intelligent status management**. The system supports goal trees, milestone tracking, bulk operations, and analytics.

## 🎯 Goal Features

The system supports:
- **Hierarchical Goals**: Parent-child relationships for complex goal structures
- **Progress Tracking**: Automatic progress calculation based on target metrics
- **Status Management**: Automatic status updates based on progress and deadlines
- **Milestone Support**: Special goal types for tracking key achievements
- **Bulk Operations**: Update multiple goals simultaneously
- **Search & Analytics**: Text search and comprehensive statistics

## 🏗️ Goal Structure

Each goal contains:
- **Basic Info**: `title`, `description`, `notes`
- **Classification**: `priority` (LOW, MEDIUM, HIGH, CRITICAL), `isMilestone`
- **Progress**: `metric` (COUNT, DURATION, CUSTOM), `targetOperator` (GREATER_THAN, EQUAL, LESS_THAN)
- **Values**: `targetValue`, `currentValue`, `progressPercentage` (auto-calculated)
- **Dates**: `startDate`, `targetDate`, `completedDate`
- **Hierarchy**: `parentGoalId` for goal trees
- **Status**: `status` (NOT_STARTED, IN_PROGRESS, COMPLETED, OVERDUE)

## 🔗 Base URL
```
http://localhost:8081/api/v1/goals
```

---

## 📚 API Endpoints

### 1. Create Goal
**Endpoint:** `POST /api/v1/goals`

**Description:** Creates a new goal with automatic progress calculation and status assignment.

**Headers:**
- `Authorization: Bearer <JWT_TOKEN>`
- `Content-Type: application/json`

**Request Body:**
```json
{
  "title": "Complete Spring Boot Course",
  "description": "Master Spring Boot framework fundamentals",
  "notes": "Focus on REST APIs and microservices",
  "priority": "HIGH",
  "metric": "COUNT",
  "targetOperator": "GREATER_THAN",
  "targetValue": 10,
  "currentValue": 0,
  "startDate": "2025-01-01T09:00:00",
  "targetDate": "2025-03-31T18:00:00",
  "parentGoalId": null,
  "isMilestone": false
}
```

**Response (201 Created):**
```json
{
  "message": "Goal created successfully",
  "status": "success",
  "data": {
    "id": 1,
    "uuid": "550e8400-e29b-41d4-a716-446655440001",
    "userId": "user-123",
    "title": "Complete Spring Boot Course",
    "description": "Master Spring Boot framework fundamentals",
    "notes": "Focus on REST APIs and microservices",
    "priority": "HIGH",
    "status": "NOT_STARTED",
    "metric": "COUNT",
    "targetOperator": "GREATER_THAN",
    "targetValue": 10.0,
    "currentValue": 0.0,
    "progressPercentage": 0.0,
    "startDate": "2025-01-01T09:00:00",
    "targetDate": "2025-03-31T18:00:00",
    "completedDate": null,
    "parentGoalId": null,
    "isMilestone": false,
    "createdAt": "2025-08-27T10:30:00",
    "lastUpdatedAt": "2025-08-27T10:30:00"
  }
}
```

---

### 2. Get All Goals
**Endpoint:** `GET /api/v1/goals`

**Description:** Retrieves all goals for the authenticated user.

**Headers:**
- `Authorization: Bearer <JWT_TOKEN>`

**Response (200 OK):**
```json
{
  "message": "Goals retrieved successfully",
  "status": "success",
  "data": [
    {
      "id": 1,
      "uuid": "550e8400-e29b-41d4-a716-446655440001",
      "title": "Complete Spring Boot Course",
      "status": "IN_PROGRESS",
      "priority": "HIGH",
      "progressPercentage": 30.0,
      "targetDate": "2025-03-31T18:00:00",
      "isMilestone": false
    }
  ]
}
```

---

### 3. Get Goal by ID
**Endpoint:** `GET /api/v1/goals/{id}`

**Description:** Retrieves a specific goal by its ID.

**Headers:**
- `Authorization: Bearer <JWT_TOKEN>`

**Parameters:**
- `id` (path): Goal ID

**Response (200 OK):**
```json
{
  "message": "Goal retrieved successfully",
  "status": "success",
  "data": {
    "id": 1,
    "uuid": "550e8400-e29b-41d4-a716-446655440001",
    "title": "Complete Spring Boot Course",
    "description": "Master Spring Boot framework fundamentals",
    "progressPercentage": 30.0,
    "status": "IN_PROGRESS",
    "childGoals": []
  }
}
```

---

### 4. Update Goal
**Endpoint:** `PUT /api/v1/goals/{id}`

**Description:** Updates an existing goal with automatic progress recalculation.

**Headers:**
- `Authorization: Bearer <JWT_TOKEN>`
- `Content-Type: application/json`

**Parameters:**
- `id` (path): Goal ID

**Request Body:**
```json
{
  "title": "Master Spring Boot & Microservices",
  "description": "Updated description with microservices focus",
  "priority": "CRITICAL",
  "currentValue": 3.0
}
```

**Response (200 OK):**
```json
{
  "message": "Goal updated successfully",
  "status": "success",
  "data": {
    "id": 1,
    "title": "Master Spring Boot & Microservices",
    "progressPercentage": 30.0,
    "status": "IN_PROGRESS",
    "lastUpdatedAt": "2025-08-27T11:45:00"
  }
}
```

---

### 5. Delete Goal
**Endpoint:** `DELETE /api/v1/goals/{id}`

**Description:** Soft deletes a goal and all its child goals.

**Headers:**
- `Authorization: Bearer <JWT_TOKEN>`

**Parameters:**
- `id` (path): Goal ID

**Response (200 OK):**
```json
{
  "message": "Goal deleted successfully",
  "status": "success",
  "data": null
}
```

---

### 6. Get Goal Tree
**Endpoint:** `GET /api/v1/goals/tree`

**Description:** Retrieves goals in hierarchical tree structure with parent-child relationships.

**Headers:**
- `Authorization: Bearer <JWT_TOKEN>`

**Response (200 OK):**
```json
{
  "message": "Goal tree retrieved successfully",
  "status": "success",
  "data": [
    {
      "id": 1,
      "title": "Learn Programming",
      "status": "IN_PROGRESS",
      "progressPercentage": 45.0,
      "childGoals": [
        {
          "id": 2,
          "title": "Master Spring Boot",
          "status": "IN_PROGRESS",
          "progressPercentage": 30.0,
          "parentGoalId": "550e8400-e29b-41d4-a716-446655440001",
          "childGoals": []
        }
      ]
    }
  ]
}
```

---

### 7. Bulk Progress Update
**Endpoint:** `PATCH /api/v1/goals/progress/bulk`

**Description:** Updates current values for multiple goals simultaneously.

**Headers:**
- `Authorization: Bearer <JWT_TOKEN>`
- `Content-Type: application/json`

**Request Body:**
```json
{
  "1": 5.0,
  "2": 8.0,
  "3": 12.0
}
```

**Response (200 OK):**
```json
{
  "message": "Progress updated successfully",
  "status": "success",
  "data": [
    {
      "id": 1,
      "progressPercentage": 50.0,
      "status": "IN_PROGRESS"
    }
  ]
}
```

---

### 8. Bulk Status Update
**Endpoint:** `PATCH /api/v1/goals/status/bulk`

**Description:** Updates status for multiple goals simultaneously.

**Headers:**
- `Authorization: Bearer <JWT_TOKEN>`
- `Content-Type: application/json`

**Request Body:**
```json
{
  "1": "COMPLETED",
  "2": "IN_PROGRESS",
  "3": "OVERDUE"
}
```

**Response (200 OK):**
```json
{
  "message": "Status updated successfully",
  "status": "success",
  "data": [
    {
      "id": 1,
      "status": "COMPLETED",
      "completedDate": "2025-08-27T12:00:00"
    }
  ]
}
```

---

### 9. Get Goal Statistics
**Endpoint:** `GET /api/v1/goals/statistics`

**Description:** Retrieves comprehensive analytics and statistics for user's goals.

**Headers:**
- `Authorization: Bearer <JWT_TOKEN>`

**Response (200 OK):**
```json
{
  "message": "Statistics retrieved successfully",
  "status": "success",
  "data": {
    "totalGoals": 15,
    "completedGoals": 6,
    "inProgressGoals": 5,
    "notStartedGoals": 2,
    "overdueGoals": 2,
    "dueSoonGoals": 3,
    "milestones": 4,
    "overallCompletionPercentage": 40.0,
    "overduePercentage": 13.33,
    "goalsByPriority": {
      "CRITICAL": 2,
      "HIGH": 5,
      "MEDIUM": 6,
      "LOW": 2
    },
    "goalsByStatus": {
      "COMPLETED": 6,
      "IN_PROGRESS": 5,
      "NOT_STARTED": 2,
      "OVERDUE": 2
    }
  }
}
```

---

### 10. Get Overdue Goals
**Endpoint:** `GET /api/v1/goals/overdue`

**Description:** Retrieves all goals that are past their target date and not completed.

**Headers:**
- `Authorization: Bearer <JWT_TOKEN>`

**Response (200 OK):**
```json
{
  "message": "Overdue goals retrieved successfully",
  "status": "success",
  "data": [
    {
      "id": 3,
      "title": "Complete Project Documentation",
      "status": "OVERDUE",
      "targetDate": "2025-08-20T18:00:00",
      "progressPercentage": 60.0,
      "priority": "HIGH"
    }
  ]
}
```

---

### 11. Get Due Soon Goals
**Endpoint:** `GET /api/v1/goals/due-soon`

**Description:** Retrieves goals due within the next 7 days.

**Headers:**
- `Authorization: Bearer <JWT_TOKEN>`

**Response (200 OK):**
```json
{
  "message": "Due soon goals retrieved successfully",
  "status": "success",
  "data": [
    {
      "id": 4,
      "title": "Prepare Monthly Report",
      "status": "IN_PROGRESS",
      "targetDate": "2025-08-30T17:00:00",
      "progressPercentage": 25.0,
      "priority": "MEDIUM"
    }
  ]
}
```

---

### 12. Search Goals
**Endpoint:** `GET /api/v1/goals/search?query={searchTerm}`

**Description:** Searches goals by title and description using text matching.

**Headers:**
- `Authorization: Bearer <JWT_TOKEN>`

**Parameters:**
- `query` (query): Search term

**Example:** `GET /api/v1/goals/search?query=spring`

**Response (200 OK):**
```json
{
  "message": "Search completed successfully",
  "status": "success",
  "data": [
    {
      "id": 1,
      "title": "Master Spring Boot",
      "description": "Learn Spring Boot framework",
      "status": "IN_PROGRESS",
      "progressPercentage": 30.0
    }
  ]
}
```

---

### 13. Get Milestones
**Endpoint:** `GET /api/v1/goals/milestones`

**Description:** Retrieves all goals marked as milestones.

**Headers:**
- `Authorization: Bearer <JWT_TOKEN>`

**Response (200 OK):**
```json
{
  "message": "Milestones retrieved successfully",
  "status": "success",
  "data": [
    {
      "id": 5,
      "title": "Complete Certification",
      "status": "NOT_STARTED",
      "isMilestone": true,
      "priority": "CRITICAL",
      "targetDate": "2025-12-31T23:59:59"
    }
  ]
}
```

---

### 14. Update Goal Progress
**Endpoint:** `PATCH /api/v1/goals/{id}/progress`

**Description:** Updates the current value of a specific goal and recalculates progress.

**Headers:**
- `Authorization: Bearer <JWT_TOKEN>`
- `Content-Type: application/json`

**Parameters:**
- `id` (path): Goal ID

**Request Body:**
```json
{
  "currentValue": 7.5
}
```

**Response (200 OK):**
```json
{
  "message": "Goal progress updated successfully",
  "status": "success",
  "data": {
    "id": 1,
    "currentValue": 7.5,
    "progressPercentage": 75.0,
    "status": "IN_PROGRESS",
    "lastUpdatedAt": "2025-08-27T14:20:00"
  }
}
```

---

### 15. Recalculate All Progress
**Endpoint:** `POST /api/v1/goals/recalculate`

**Description:** Recalculates progress and updates status for all user goals.

**Headers:**
- `Authorization: Bearer <JWT_TOKEN>`

**Response (200 OK):**
```json
{
  "message": "Progress recalculated successfully",
  "status": "success",
  "data": null
}
```

---

## 🎯 Goal Progress Calculation

### Progress Formula by Target Operator:

1. **GREATER_THAN**: `(currentValue / targetValue) * 100`
   - Example: Target 10, Current 3 = 30%

2. **EQUAL**: `currentValue == targetValue ? 100 : 0`
   - Example: Target 5, Current 5 = 100%

3. **LESS_THAN**: `currentValue <= targetValue ? 100 : max(0, 100 - ((currentValue - targetValue) / targetValue) * 100)`
   - Example: Target 30min, Current 25min = 100%

### Automatic Status Updates:

- **COMPLETED**: When progress reaches 100%
- **OVERDUE**: When target date passes and not completed
- **IN_PROGRESS**: When current value > 0 and < 100%
- **NOT_STARTED**: Initial state

---

## 🔐 Authentication

All endpoints require JWT authentication:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

The JWT token must contain:
- `sub`: User ID claim
- Valid signature
- Non-expired timestamp

---

## ❌ Error Responses

### 400 Bad Request
```json
{
  "status": "error",
  "message": "Goal title is required",
  "data": null
}
```

### 401 Unauthorized
```json
{
  "status": "error",
  "message": "User not authenticated",
  "data": null
}
```

### 404 Not Found
```json
{
  "status": "error",
  "message": "Goal not found with ID: 999",
  "data": null
}
```

### 500 Internal Server Error
```json
{
  "status": "error",
  "message": "An unexpected error occurred",
  "data": null
}
```

---

## 📊 Usage Examples

### Creating a Goal Hierarchy
```bash
# 1. Create parent goal
curl -X POST http://localhost:8081/api/v1/goals \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Learn Programming",
    "priority": "HIGH",
    "metric": "COUNT",
    "targetOperator": "GREATER_THAN",
    "targetValue": 5
  }'

# 2. Create child goal
curl -X POST http://localhost:8081/api/v1/goals \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Master Spring Boot",
    "parentGoalId": "550e8400-e29b-41d4-a716-446655440001",
    "priority": "MEDIUM",
    "metric": "COUNT",
    "targetOperator": "GREATER_THAN",
    "targetValue": 10
  }'
```

### Tracking Progress
```bash
# Update progress
curl -X PATCH http://localhost:8081/api/v1/goals/1/progress \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"currentValue": 8.5}'

# Check statistics
curl -X GET http://localhost:8081/api/v1/goals/statistics \
  -H "Authorization: Bearer <token>"
```

---

## 🚀 Best Practices

1. **Use Meaningful Titles**: Make goal titles descriptive and actionable
2. **Set Realistic Targets**: Choose achievable target values and dates
3. **Regular Updates**: Update progress frequently for accurate tracking
4. **Utilize Hierarchy**: Break large goals into smaller, manageable sub-goals
5. **Monitor Deadlines**: Check overdue and due-soon endpoints regularly
6. **Leverage Milestones**: Mark important achievements as milestones
