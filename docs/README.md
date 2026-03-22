# Northstar Activity Tracker — Technical Overview

## What This System Does

The Northstar Activity Tracker is a comprehensive goal and activity management system designed to help users track, measure, and improve their personal and professional objectives. The system allows users to create hierarchical goal structures, log daily activities, and automatically calculates health scores based on consistency, momentum, and progress. Users can organize goals into categories (Domain → Subdomain → Specific), set up flexible scheduling, and track progress through multiple evaluation periods (daily, weekly, monthly, quarterly, yearly, or custom). The system provides real-time health scoring, streak tracking, and parent goal rollup insights to give users a complete picture of their goal achievement journey.

Built with a focus on flexibility and extensibility, the system supports both simple tracking needs (like "exercise 3 times per week") and complex hierarchical goals (like "Complete MBA" with sub-goals for CAT preparation, quantitative practice, and reading). The health score engine provides intelligent feedback on goal performance, helping users identify areas needing attention while celebrating consistent progress through streaks and achievement milestones.

## Tech Stack

- **Java Version**: 21
- **Spring Boot Version**: 3.4.3
- **Database**: PostgreSQL with Hibernate/JPA
- **Security**: Spring Security with OAuth2/JWT (Resource Server)
- **Build Tool**: Maven
- **Additional**: Kafka for messaging, Lombok for boilerplate reduction

## How to Run Locally

### Prerequisites
- Java 21 installed
- PostgreSQL running on localhost:5432
- Kafka running on localhost:9092 (optional, for messaging features)
- OAuth2 Auth Server running on localhost:8082

### Environment Setup
1. Create PostgreSQL database named `ActivityData`
2. Create user `sagar` with password (empty in current config)
3. Ensure Kafka is running if using messaging features

### Running the Application
```bash
# Navigate to project root
cd /Users/sagar/Desktop/Personal/Activity-Tracker/Northstar-main

# Run the application
mvn spring-boot:run

# Or compile and run
mvn clean compile
java -jar target/activity-tracker-0.0.1-SNAPSHOT.jar
```

### Default Configuration
- **Server Port**: 8081
- **Database**: jdbc:postgresql://localhost:5432/ActivityData
- **Auth Server**: http://localhost:8082
- **Kafka**: localhost:9092

## Project Structure

```
src/main/java/com/sagarpandey/activity_tracker/
├── ActivityTrackerApplication.java          # Main Spring Boot application
├── controllers/                              # REST API endpoints
│   ├── ActivityController.java              # Activity CRUD operations
│   ├── CategoryController.java              # Domain/Subdomain/Specific management
│   └── GoalController.java                  # Goal CRUD and health endpoints
├── models/                                  # JPA entities
│   ├── BaseModel.java                       # Base entity with common fields
│   ├── Activity.java                        # Activity records
│   ├── Domain.java                          # Top-level categories
│   ├── Subdomain.java                       # Mid-level categories
│   ├── Specifics.java                       # Bottom-level categories
│   ├── Goal.java                            # Goal entities with health tracking
│   ├── GoalWeeklySnapshot.java              # Weekly activity snapshots
│   └── GoalPeriodSnapshot.java              # Period-based activity snapshots
├── Repository/                              # Data access layer
│   ├── ActivityRepository.java
│   ├── DomainRepository.java
│   ├── SubdomainRepository.java
│   ├── SpecificsRepository.java
│   ├── GoalRepository.java
│   ├── GoalWeeklySnapshotRepository.java
│   └── GoalPeriodSnapshotRepository.java
├── Service/                                 # Business logic layer
│   ├── Interface/                           # Service interfaces
│   │   ├── ActivityServiceInterface.java
│   │   ├── DomainServiceInterface.java
│   │   ├── GoalService.java
│   │   ├── GoalHealthService.java
│   │   ├── RollupService.java
│   │   └── SpecificsServicesInterface.java
│   └── V1/                                 # Service implementations
│       ├── ActivityServiceV1.java
│       ├── DomainServiceV1.java
│       ├── GoalServiceV1.java
│       ├── GoalHealthServiceV1.java
│       ├── RollupServiceV1.java
│       └── SpecificsServiceV1.java
├── Mapper/                                  # Entity-DTO mapping
│   ├── ActivityMapper.java
│   ├── GoalMapper.java
│   └── ParentInsights.java                  # DTO for parent goal insights
├── dtos/                                    # Data Transfer Objects
│   ├── ActivityRequest.java
│   ├── ActivityResponse.java
│   ├── ActivitySearchRequest.java
│   ├── ActivitySearchResponse.java
│   ├── ActivityBulkCreateRequest.java
│   ├── ActivityBulkCreateResponse.java
│   ├── CategoryRequest.java
│   ├── CategoryResponse.java
│   ├── GoalRequest.java
│   ├── GoalResponse.java
│   ├── GoalStatsResponse.java
│   ├── ParentInsights.java
│   ├── ResponseWrapper.java
│   └── ValidationErrorMessage.java
├── enums/                                  # System enums
│   ├── ActivitySource.java
│   ├── EvaluationPeriod.java               # Period types for goals
│   ├── GoalType.java                       # Goal categories
│   ├── HealthStatus.java                   # Health score status levels
│   ├── ScheduleDay.java                    # Days of week for scheduling
│   └── ScheduleType.java                   # Schedule configuration types
├── utils/                                  # Utility classes
│   ├── PeriodUtils.java                    # Period calculation utilities
│   └── WeekUtils.java                      # Week-based calculations
├── jobs/                                   # Scheduled tasks
│   └── WeeklySnapshotResetJob.java         # Weekly health recalculation
├── Exceptions/                             # Custom exceptions
│   ├── DurationCalculationException.java
│   ├── ErrorWhileProcessing.java
│   ├── GoalNotFoundException.java
│   └── ValidationException.java
└── validators/                             # Input validation
    └── GoalWeightValidator.java
```

## Quick Links to Other Docs

- [Data Model](DATA_MODEL.md) - Entity relationships and database schema
- [Goal System](GOAL_SYSTEM.md) - Goal hierarchy, types, and configuration
- [Health Score Engine](HEALTH_SCORE_ENGINE.md) - Health calculation algorithms
- [API Reference](API_REFERENCE.md) - Complete API documentation
- [Frontend Contract](FRONTEND_CONTRACT.md) - Backend vs frontend responsibilities
- [Architecture Decisions](ARCHITECTURE_DECISIONS.md) - Key design decisions
- [Phases Changelog](PHASES_CHANGELOG.md) - Development phases and features
