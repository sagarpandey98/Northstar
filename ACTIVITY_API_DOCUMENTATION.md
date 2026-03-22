# Activity Tracking API Documentation

## 📋 Overview

The Activity Tracking API provides a comprehensive system for logging and managing daily activities with **automatic duration calculation**, **mood and rating tracking**, and **category-based organization**. The system enables users to track their time usage, productivity, and personal insights.

## ⏱️ Activity Features

The system supports:
- **Time Tracking**: Precise start and end time recording with automa**3. Category + Mood Filter - Fitness Activities with Good Mood:**
```bash
curl -X POST "http://localhost:8081/api/v1/activities/search" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "filter": {
      "domainName": "fitness",
      "minMood": 4,
      "maxMood": 5
    },
    "pagination": {
      "page": 0,
      "size": 10,
      "sortBy": "mood",
      "sortDirection": "DESC"
    }
  }'
```ation
- **Categorization**: Primary and secondary categories for activity organization
- **Mood & Rating**: Subjective well-being and activity quality tracking (1-5 scale)
- **Descriptions**: Detailed notes and context for activities
- **User Isolation**: Activities are strictly filtered by user authentication

## 🏗️ Activity Structure

Each activity contains:
- **Basic Info**: `name`, `description`
- **Time Data**: `startTime`, `endTime` (with timezone support)
- **Categories**: `primaryCategory`, `secondaryCategory` (linked to category management)
- **Subjective Data**: `mood` (1-5), `rating` (1-5)
- **Metadata**: `id`, `created_at`, auto-calculated duration

## 🔗 Base URL
```
http://localhost:8081/api/v1/activities
```

---

## 📚 API Endpoints

### 1. Create Activity
**Endpoint:** `POST /api/v1/activities`

**Description:** Creates a new activity with automatic duration calculation and validation.

**Headers:**
- `Authorization: Bearer <JWT_TOKEN>`
- `Content-Type: application/json`

**Request Body:**
```json
{
  "data": {
    "name": "Morning Workout",
    "startTime": "2025-08-27T06:00:00+00:00",
    "endTime": "2025-08-27T07:30:00+00:00",
    "description": "Cardio and strength training session",
    "domainId": "550e8400-e29b-41d4-a716-446655440001",
    "domainName": "Health",
    "subdomainId": "550e8400-e29b-41d4-a716-446655440002",
    "subdomainName": "Fitness", 
    "specificId": "550e8400-e29b-41d4-a716-446655440003",
    "specificName": "Cardio Training",
    "mood": 4,
    "rating": 5,
    "source": "API_SINGLE"
  }
}
```

**Validation Rules:**
- `name`: Required, non-blank
- `startTime`: Required, valid ISO 8601 with timezone
- `endTime`: Required, must be after startTime
- `domainId`: Required, UUID of the domain
- `domainName`: Required, name of the domain
- `subdomainId`: Required, UUID of the subdomain
- `subdomainName`: Required, name of the subdomain
- `specificId`: Required, UUID of the specific activity type
- `specificName`: Required, name of the specific activity type
- `mood`: Optional, integer 1-5
- `rating`: Optional, integer 1-5
- `description`: Optional, activity details
- `source`: Optional, defaults to "API_SINGLE" if not provided

**Response (201 Created):**
```json
{
  "name": "Morning Workout",
  "startTime": "2025-08-27T06:00:00+00:00",
  "endTime": "2025-08-27T07:30:00+00:00",
  "description": "Cardio and strength training session",
  "id": 1,
  "created_at": "2025-08-27T10:30:00"
}
```

**Error Responses:**

**400 Bad Request - Validation Error:**
```json
{
  "status": "error",
  "message": "Activity name is required",
  "data": null
}
```

**400 Bad Request - Duration Error:**
```json
{
  "status": "error",
  "message": "End time must be after start time",
  "data": null
}
```

---

### 2. Bulk Create Activities
**Endpoint:** `POST /api/v1/activities/bulk`

**Description:** Creates multiple activities in a single request. Returns detailed information about successful and failed creations. User ID is automatically extracted from the JWT token.

**Headers:**
- `Authorization: Bearer <JWT_TOKEN>`
- `Content-Type: application/json`

**Request Body:**
```json
{
  "activities": [
    {
      "name": "Morning Workout",
      "startTime": "2025-08-27T06:00:00+00:00",
      "endTime": "2025-08-27T07:30:00+00:00",
      "description": "Cardio and strength training session",
      "domainId": "550e8400-e29b-41d4-a716-446655440001",
      "domainName": "Health",
      "subdomainId": "550e8400-e29b-41d4-a716-446655440002",
      "subdomainName": "Fitness",
      "specificId": "550e8400-e29b-41d4-a716-446655440003",
      "specificName": "Cardio Training",
      "mood": 4,
      "rating": 5
    },
    {
      "name": "Team Meeting",
      "startTime": "2025-08-27T09:00:00+00:00",
      "endTime": "2025-08-27T10:00:00+00:00",
      "description": "Weekly project standup",
      "domainId": "550e8400-e29b-41d4-a716-446655440004",
      "domainName": "Work",
      "subdomainId": "550e8400-e29b-41d4-a716-446655440005",
      "subdomainName": "Management",
      "specificId": "550e8400-e29b-41d4-a716-446655440006",
      "specificName": "Team Meeting",
      "mood": 3,
      "rating": 4
    }
  ]
}
```

**Validation Rules:**
- `activities`: Required, non-empty array
- Each activity follows the same validation rules as single activity creation

**Response (201 Created - All Successful):**
```json
{
  "successful": [
    {
      "id": 1,
      "name": "Morning Workout",
      "startTime": "2025-08-27T06:00:00+00:00",
      "endTime": "2025-08-27T07:30:00+00:00",
      "description": "Cardio and strength training session",
      "created_at": "2025-08-27T10:30:00",
      "domainId": "550e8400-e29b-41d4-a716-446655440001",
      "domainName": "Health",
      "subdomainId": "550e8400-e29b-41d4-a716-446655440002",
      "subdomainName": "Fitness",
      "specificId": "550e8400-e29b-41d4-a716-446655440003",
      "specificName": "Cardio Training",
      "mood": 4,
      "rating": 5,
      "duration": "1 hours, 30 minutes"
    },
    {
      "id": 2,
      "name": "Team Meeting",
      "startTime": "2025-08-27T09:00:00+00:00",
      "endTime": "2025-08-27T10:00:00+00:00",
      "description": "Weekly project standup",
      "created_at": "2025-08-27T10:31:00",
      "domainId": "550e8400-e29b-41d4-a716-446655440004",
      "domainName": "Work",
      "subdomainId": "550e8400-e29b-41d4-a716-446655440005",
      "subdomainName": "Management",
      "specificId": "550e8400-e29b-41d4-a716-446655440006",
      "specificName": "Team Meeting",
      "mood": 3,
      "rating": 4,
      "duration": "1 hours, 0 minutes"
    }
  ],
  "failed": [],
  "totalRequested": 2,
  "totalSuccessful": 2,
  "totalFailed": 0
}
```

**Response (207 Multi-Status - Partial Success):**
```json
{
  "successful": [
    {
      "id": 1,
      "name": "Morning Workout",
      "startTime": "2025-08-27T06:00:00+00:00",
      "endTime": "2025-08-27T07:30:00+00:00",
      "description": "Cardio and strength training session",
      "created_at": "2025-08-27T10:30:00"
    }
  ],
  "failed": [
    {
      "activityData": {
        "name": "Invalid Activity",
        "startTime": "invalid-date",
        "endTime": "2025-08-27T10:00:00+00:00"
      },
      "errorMessage": "Invalid start time format",
      "errorCode": "CREATION_FAILED"
    }
  ],
  "totalRequested": 2,
  "totalSuccessful": 1,
  "totalFailed": 1
}
```

**Response (400 Bad Request - All Failed):**
```json
{
  "successful": [],
  "failed": [
    {
      "activityData": {
        "name": "",
        "startTime": "2025-08-27T06:00:00+00:00",
        "endTime": "2025-08-27T07:30:00+00:00"
      },
      "errorMessage": "Activity name is required",
      "errorCode": "CREATION_FAILED"
    }
  ],
  "totalRequested": 1,
  "totalSuccessful": 0,
  "totalFailed": 1
}
```

---

### 3. Search/Filter Activities
**Endpoint:** `POST /api/v1/activities/search`

**Description:** Universal endpoint for searching and filtering activities with pagination support. Supports complex filtering by time range, activity fields, and search terms. User ID is automatically extracted from the JWT token.

**Headers:**
- `Authorization: Bearer <JWT_TOKEN>`
- `Content-Type: application/json`

**Request Body:**
```json
{
  "filter": {
    "startTime": "2025-08-27T00:00:00+00:00",
    "endTime": "2025-08-27T23:59:59+00:00",
    "name": "workout",
    "primaryCategory": "fitness",
    "secondaryCategory": "cardio",
    "tertiaryCategory": "running",
    "description": "morning",
    "minMood": 3,
    "maxMood": 5,
    "minRating": 4,
    "maxRating": 5,
    "specificActivityId": "abc123",
    "searchTerm": "workout fitness"
  },
  "pagination": {
    "page": 0,
    "size": 10,
    "sortBy": "createdAt",
    "sortDirection": "DESC"
  }
}
```

**Filter Options:**
- **Time Range**: `startTime`, `endTime` - Filter activities within date range
- **Activity Fields**: `name`, `primaryCategory`, `secondaryCategory`, `tertiaryCategory`, `description` - Text-based filtering with partial matching
- **Mood Range**: `minMood`, `maxMood` - Filter by mood range (1-5)
- **Rating Range**: `minRating`, `maxRating` - Filter by rating range (1-5)
- **Specific ID**: `specificActivityId` - Exact match for specific activity ID
- **General Search**: `searchTerm` - Search across name, description, and categories

**Pagination Options:**
- **page**: Page number (0-based, default: 0)
- **size**: Items per page (default: 10, options: 10, 20, 30, etc.)
- **sortBy**: Sort field (default: "createdAt", options: "name", "startTime", "endTime", "createdAt")
- **sortDirection**: Sort direction (default: "DESC", options: "ASC", "DESC")

**Response:**
```json
{
  "activities": [
    {
      "id": 123,
      "name": "Morning Workout",
      "startTime": "2025-08-27T06:00:00+00:00",
      "endTime": "2025-08-27T07:30:00+00:00",
      "description": "Cardio and strength training session",
      "created_at": "2025-08-27T05:45:30"
    }
  ],
  "pagination": {
    "currentPage": 0,
    "pageSize": 10,
    "totalElements": 45,
    "totalPages": 5,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

**Example Use Cases:**

1. **Time Range Search**: Find all activities between specific dates
```json
{
  "filter": {
    "startTime": "2025-08-27T00:00:00+00:00",
    "endTime": "2025-08-27T23:59:59+00:00"
  },
  "pagination": { "page": 0, "size": 20 }
}
```

2. **Category + Mood Filter**: Find fitness activities with good mood
```json
{
  "filter": {
    "primaryCategory": "fitness",
    "minMood": 4
  },
  "pagination": { "page": 0, "size": 10, "sortBy": "startTime", "sortDirection": "DESC" }
}
```

3. **General Search**: Search across multiple fields
```json
{
  "filter": {
    "searchTerm": "workout morning fitness"
  },
  "pagination": { "page": 0, "size": 15 }
}
```

4. **High-Quality Activities**: Find highly rated activities
```json
{
  "filter": {
    "minRating": 4,
    "maxRating": 5
  },
  "pagination": { "page": 0, "size": 10, "sortBy": "rating", "sortDirection": "DESC" }
}
```

### 4. Get All Activities
**Endpoint:** `GET /api/v1/activities`

**Description:** Retrieves all activities for the authenticated user. User ID is automatically extracted from the JWT token.

**Headers:**
- `Authorization: Bearer <JWT_TOKEN>`

**Response (200 OK):**
```json
[
  {
    "name": "Morning Workout",
    "startTime": "2025-08-27T06:00:00+00:00",
    "endTime": "2025-08-27T07:30:00+00:00",
    "description": "Cardio and strength training session",
    "id": 1,
    "created_at": "2025-08-27T10:30:00"
  },
  {
    "name": "Team Meeting",
    "startTime": "2025-08-27T09:00:00+00:00",
    "endTime": "2025-08-27T10:00:00+00:00",
    "description": "Weekly project standup",
    "id": 2,
    "created_at": "2025-08-27T11:15:00"
  }
]
```

---

## ⏰ Time Format & Duration

### Time Format
All timestamps use **ISO 8601 format with timezone offset**:
```
YYYY-MM-DDTHH:mm:ss+/-HH:mm
```

**Examples:**
- UTC: `2025-08-27T14:30:00+00:00`
- EST: `2025-08-27T09:30:00-05:00`
- IST: `2025-08-27T20:00:00+05:30`

### Duration Calculation
The system automatically calculates duration when creating activities:
- **Duration = endTime - startTime**
- Minimum duration: 1 minute
- Maximum duration: 24 hours
- Cross-midnight activities are supported

---

## 📊 Activity Categories Integration

Activities can be linked to the Category Management system:

### Category Linking
```json
{
  "primaryCategory": "Health",      // Domain level
  "secondaryCategory": "Fitness"    // SubDomain level
}
```

### Category Validation
- Categories are validated against existing category structure
- Invalid categories are accepted but flagged for review
- Empty categories are allowed for quick logging

---

## 🎭 Mood & Rating System

### Mood Scale (1-5):
- **1**: Very Poor - Feeling terrible, low energy
- **2**: Poor - Below average, somewhat negative
- **3**: Neutral - Average, neither good nor bad
- **4**: Good - Above average, positive feeling
- **5**: Excellent - Outstanding, very high energy

### Rating Scale (1-5):
- **1**: Very Poor - Unproductive, wasteful time
- **2**: Poor - Below expectations, limited value
- **3**: Average - Met basic expectations
- **4**: Good - Above expectations, valuable time
- **5**: Excellent - Extremely productive, high value

### Usage Example:
```json
{
  "name": "Deep Work Session",
  "mood": 4,     // Felt good during the activity
  "rating": 5    // Extremely productive session
}
```

---

## 🔐 Authentication & Security

### JWT Authentication
All endpoints require valid JWT authentication:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### User Isolation
- Activities are strictly filtered by user ID from JWT
- Users can only access their own activities
- Cross-user data access is prevented

---

## ❌ Error Handling

### 400 Bad Request - Invalid Data
```json
{
  "status": "error",
  "message": "Activity start time is required",
  "data": null
}
```

### 400 Bad Request - Duration Error
```json
{
  "status": "error",
  "message": "Activity duration cannot exceed 24 hours",
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

### 500 Internal Server Error
```json
{
  "status": "error",
  "message": "Error while processing activity data",
  "data": null
}
```

---

## 📊 Usage Examples

### Basic Activity Logging
```bash
# Log a work session
curl -X POST http://localhost:8081/api/v1/activities \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "data": {
      "name": "Code Review",
      "startTime": "2025-08-27T14:00:00+00:00",
      "endTime": "2025-08-27T15:30:00+00:00",
      "description": "Reviewed PRs for authentication module",
      "domainId": "550e8400-e29b-41d4-a716-446655440007",
      "domainName": "Work",
      "subdomainId": "550e8400-e29b-41d4-a716-446655440008",
      "subdomainName": "Development",
      "specificId": "550e8400-e29b-41d4-a716-446655440009",
      "specificName": "Backend Development",
      "mood": 3,
      "rating": 4
    }
  }'
```

### Retrieve All Activities
```bash
curl -X GET http://localhost:8081/api/v1/activities \
  -H "Authorization: Bearer <token>"
```

### Fitness Activity Example
```bash
curl -X POST http://localhost:8081/api/v1/activities \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "data": {
      "name": "Evening Run",
      "startTime": "2025-08-27T18:00:00+05:30",
      "endTime": "2025-08-27T18:45:00+05:30",
      "description": "5K run in the park",
      "domainId": "550e8400-e29b-41d4-a716-446655440010",
      "domainName": "Health",
      "subdomainId": "550e8400-e29b-41d4-a716-446655440011",
      "subdomainName": "Cardio",
      "specificId": "550e8400-e29b-41d4-a716-446655440012",
      "specificName": "Running",
      "mood": 5,
      "rating": 4
    }
  }'
```

### Bulk Create Activities Example
```bash
curl -X POST http://localhost:8081/api/v1/activities/bulk \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "activities": [
      {
        "name": "Morning Workout",
        "startTime": "2025-08-27T06:00:00+00:00",
        "endTime": "2025-08-27T07:30:00+00:00",
        "description": "Cardio and strength training session",
        "domainId": 1,
        "domainName": "Health",
        "subdomainId": 2,
        "subdomainName": "Fitness",
        "specificId": 3,
        "specificName": "Cardio Training",
        "mood": 4,
        "rating": 5
      },
      {
        "name": "Work Session",
        "startTime": "2025-08-27T09:00:00+00:00",
        "endTime": "2025-08-27T12:00:00+00:00",
        "description": "Deep focus coding session",
        "domainId": "550e8400-e29b-41d4-a716-446655440013",
        "domainName": "Work",
        "subdomainId": "550e8400-e29b-41d4-a716-446655440014",
        "subdomainName": "Development",
        "specificId": "550e8400-e29b-41d4-a716-446655440015",
        "specificName": "Coding",
        "mood": 4,
        "rating": 5
      }
    ]
  }'
```

---

## 🧪 Testing with cURL

### Search API Test Commands

**1. Basic Search - Get All Activities with Pagination:**
```bash
curl -X POST "http://localhost:8081/api/v1/activities/search" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "pagination": {
      "page": 0,
      "size": 10,
      "sortBy": "createdAt",
      "sortDirection": "DESC"
    }
  }'
```

**2. Time Range Search - Activities from Today:**
```bash
curl -X POST "http://localhost:8081/api/v1/activities/search" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "filter": {
      "startTime": "2025-09-06T00:00:00+00:00",
      "endTime": "2025-09-06T23:59:59+00:00"
    },
    "pagination": {
      "page": 0,
      "size": 20
    }
  }'
```

**3. Category + Mood Filter - Fitness Activities with Good Mood:**
```bash
curl -X POST "http://localhost:8081/api/v1/activities/search" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "filter": {
      "primaryCategory": "fitness",
      "minMood": 4,
      "maxMood": 5
    },
    "pagination": {
      "page": 0,
      "size": 10,
      "sortBy": "mood",
      "sortDirection": "DESC"
    }
  }'
```

**4. General Search - Search Across Multiple Fields:**
```bash
curl -X POST "http://localhost:8081/api/v1/activities/search" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "filter": {
      "searchTerm": "workout morning fitness"
    },
    "pagination": {
      "page": 0,
      "size": 15,
      "sortBy": "startTime",
      "sortDirection": "DESC"
    }
  }'
```

**5. High-Quality Activities - Find Top Rated Activities:**
```bash
curl -X POST "http://localhost:8081/api/v1/activities/search" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "filter": {
      "minRating": 4,
      "maxRating": 5
    },
    "pagination": {
      "page": 0,
      "size": 10,
      "sortBy": "rating",
      "sortDirection": "DESC"
    }
  }'
```

**6. Complex Filter - Multiple Criteria:**
```bash
curl -X POST "http://localhost:8081/api/v1/activities/search" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "filter": {
      "startTime": "2025-09-01T00:00:00+00:00",
      "endTime": "2025-09-06T23:59:59+00:00",
      "primaryCategory": "work",
      "minRating": 3,
      "name": "meeting"
    },
    "pagination": {
      "page": 0,
      "size": 25,
      "sortBy": "startTime",
      "sortDirection": "ASC"
    }
  }'
```

**7. Name-Based Search - Find Specific Activity Types:**
```bash
curl -X POST "http://localhost:8081/api/v1/activities/search" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "filter": {
      "name": "workout"
    },
    "pagination": {
      "page": 0,
      "size": 10
    }
  }'
```

**8. Description Search - Find by Activity Details:**
```bash
curl -X POST "http://localhost:8081/api/v1/activities/search" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "filter": {
      "description": "cardio"
    },
    "pagination": {
      "page": 0,
      "size": 10
    }
  }'
```

**9. Pagination Test - Get Second Page:**
```bash
curl -X POST "http://localhost:8081/api/v1/activities/search" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "pagination": {
      "page": 1,
      "size": 10,
      "sortBy": "createdAt",
      "sortDirection": "DESC"
    }
  }'
```

**10. Empty Filter - All Activities with Custom Sort:**
```bash
curl -X POST "http://localhost:8081/api/v1/activities/search" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "filter": {},
    "pagination": {
      "page": 0,
      "size": 30,
      "sortBy": "name",
      "sortDirection": "ASC"
    }
  }'
```

### Create Activity for Testing

**First, create some test activities:**
```bash
curl -X POST "http://localhost:8081/api/v1/activities" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "data": {
      "name": "Morning Workout",
      "startTime": "2025-09-06T06:00:00+00:00",
      "endTime": "2025-09-06T07:30:00+00:00",
      "description": "Cardio and strength training session",
      "domainId": "550e8400-e29b-41d4-a716-446655440001",
      "domainName": "Health",
      "subdomainId": "550e8400-e29b-41d4-a716-446655440002",
      "subdomainName": "Fitness",
      "specificId": "550e8400-e29b-41d4-a716-446655440003",
      "specificName": "Cardio Training",
      "mood": 4,
      "rating": 5
    }
  }'
```

### Notes for Testing:
- Replace `YOUR_JWT_TOKEN` with your actual JWT token
- Change `user_id` from `1` to your actual user ID
- Adjust dates to match your test data
- The API supports all filter combinations - mix and match as needed

---

## � Activity Sources

The `source` field tracks how each activity was created in the system. This helps with analytics, debugging, and understanding user behavior patterns.

### Available Source Types:

1. **API_SINGLE** (Default)
   - Activities created through single activity API endpoint
   - Automatically set when `source` is not specified in single create requests

2. **API_BULK**
   - Activities created through bulk creation API endpoint
   - Automatically set for bulk operations when `source` is not specified

3. **IMPORT**
   - Activities imported from external sources (CSV, JSON, etc.)
   - Use when importing historical data

4. **MANUAL**
   - Activities created manually by admin users
   - For administrative data entry

5. **MOBILE_APP**
   - Activities created via mobile application
   - Track mobile vs web usage

6. **WEB_APP**
   - Activities created via web application
   - Distinguish from mobile usage

7. **SCHEDULED**
   - Activities created by automated/scheduled processes
   - For recurring activities or system-generated entries

8. **MIGRATION**
   - Activities created during data migration
   - Track legacy data imports

### Usage Examples:

**Specify source in single create:**
```json
{
  "data": {
    "name": "Imported Workout",
    "source": "IMPORT",
    // ... other fields
  }
}
```

**Specify source in bulk create:**
```json
{
  "activities": [
    {
      "name": "Legacy Activity",
      "source": "MIGRATION",
      // ... other fields
    }
  ]
}
```

**Default behavior:**
- Single API calls: `source` defaults to `"API_SINGLE"`
- Bulk API calls: `source` defaults to `"API_BULK"`
- If explicitly provided, the provided value is used

---

## �🚀 Best Practices

### Time Tracking
1. **Real-time Logging**: Log activities as they happen for accuracy
2. **Timezone Consistency**: Use consistent timezone offsets
3. **Reasonable Durations**: Ensure realistic start/end times
4. **Overlap Handling**: Avoid overlapping activities

### Data Quality
1. **Descriptive Names**: Use clear, searchable activity names
2. **Meaningful Descriptions**: Add context for future reference
3. **Consistent Categories**: Use standardized category names
4. **Honest Ratings**: Provide authentic mood and rating data

### Performance
1. **Batch Creation**: For historical data, consider batch operations
2. **Efficient Retrieval**: Use appropriate date ranges for queries
3. **Category Sync**: Ensure categories exist before referencing

---

## 🔄 Integration with Other APIs

### Category Management
Activities reference categories from the Category API:
```
GET /api/v1/categories/{userId} → Get available categories
POST /api/v1/activities → Reference categories in activity
```

### Goal Management
Activities can contribute to goal progress:
```
Activity duration → Goal progress update
Activity categories → Goal category alignment
```

### Future Enhancements
- **Filtering & Search**: Filter by date range, category, rating
- **Analytics**: Duration summaries, productivity insights
- **Bulk Operations**: Create/update multiple activities
- **Templates**: Recurring activity templates
- **Export**: CSV/JSON data export capabilities
