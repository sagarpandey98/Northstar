# 🎯 Northstar Activity Tracker

> A comprehensive Spring Boot application for personal productivity, goal management, and activity tracking.

## 🚀 Quick Start

```bash
# Clone the repository
git clone <repository-url>

# Configure database in application.properties
# Set JWT issuer URI

# Run the application
mvn spring-boot:run

# Access APIs at http://localhost:8081
```

## 📚 API Documentation

- 📄 [Goal Management API](GOAL_API_DOCUMENTATION.md) - Complete goal tracking system
- 📄 [Activity Tracking API](ACTIVITY_API_DOCUMENTATION.md) - Time and activity logging
- 📄 [Category Management API](CATEGORY_API_DOCUMENTATION.md) - Hierarchical categorization

## 🏗️ Features

### ✅ Goal Management
- Hierarchical goal structures
- Automatic progress calculation
- Status management & analytics
- Bulk operations & search

### ✅ Activity Tracking
- Time-based logging with timezone support
- Mood & rating tracking
- Category integration
- Duration auto-calculation

### ✅ Category System
- 3-level hierarchy (Domain → SubDomain → Specific)
- Intelligent change detection
- Batch operations

### ✅ Security & Auth
- JWT-based authentication
- User data isolation
- OAuth2 resource server

## 🛠️ Tech Stack

- **Backend**: Spring Boot 3.x + Jakarta EE
- **Database**: PostgreSQL + Spring Data JPA
- **Security**: OAuth2 + JWT
- **Build**: Maven + Java 23
- **Testing**: JUnit 5 + Mockito

## 📊 API Endpoints

### Goals (`/api/v1/goals`)
```
POST   /                    # Create goal
GET    /                    # Get all goals
GET    /{id}               # Get goal by ID
PUT    /{id}               # Update goal
DELETE /{id}               # Delete goal
GET    /tree               # Hierarchical structure
GET    /statistics         # Analytics dashboard
GET    /search?query=      # Search goals
```

### Activities (`/api/v1/activities`)
```
POST   /                    # Create activity
GET    /{user_id}          # Get user activities
```

### Categories (`/api/v1/categories`)
```
GET    /{userId}           # Get category structure
POST   /{userId}           # Update categories
```

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=GoalServiceV1Test

# Generate test reports
mvn surefire-report:report
```

## 📝 Project Structure

```
src/main/java/com/sagarpandey/activity_tracker/
├── controllers/     # REST endpoints
├── models/         # JPA entities
├── dtos/           # Request/Response objects
├── Repository/     # Data access layer
├── Service/        # Business logic
├── Mapper/         # Entity-DTO mapping
├── Exceptions/     # Custom exceptions
└── Security/       # Auth configuration
```

## 📋 Requirements

- Java 23+
- PostgreSQL
- Maven 3.6+

## 🔧 Configuration

Create `application.properties`:
```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/activity_tracker
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# Security
spring.security.oauth2.resourceserver.jwt.issuer-uri=${JWT_ISSUER_URI}

# Server
server.port=8081
```

## 📖 Documentation Files

- `PROJECT_SUMMARY.md` - Complete project overview
- `GOAL_API_DOCUMENTATION.md` - Goal management endpoints
- `ACTIVITY_API_DOCUMENTATION.md` - Activity tracking endpoints
- `CATEGORY_API_DOCUMENTATION.md` - Category management endpoints

---

**Author**: Sagar Pandey  
**Version**: 1.0.0  
**Last Updated**: August 27, 2025
