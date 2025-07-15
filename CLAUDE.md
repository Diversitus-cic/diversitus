# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Diversitus is a neurodiversity job matching platform that connects job seekers with suitable opportunities based on trait compatibility. The system uses a Euclidean distance algorithm to calculate similarity scores between user profiles and job requirements.

## Architecture

- **Backend**: Kotlin/Ktor REST API deployed on AWS ECS
- **Database**: DynamoDB with three tables (jobs, companies, users)
- **Infrastructure**: Pulumi TypeScript for AWS resource management
- **Containerization**: Docker with Amazon Corretto 17 JVM

### Key Components

1. **Matching Engine** (`MatchingService.kt`): Core algorithm that combines company and job traits, calculates Euclidean distance, and converts to similarity scores (0-1 range)
2. **Data Layer**: Repository pattern for DynamoDB operations with AWS SDK for Kotlin
3. **API Layer**: Ktor with OpenAPI documentation support
4. **Models**: `User`, `Company`, `Job`, `NeurodiversityProfile`, `MatchResult`

### Data Flow

1. Jobs inherit traits from their parent companies
2. Job-specific traits override company traits when conflicts exist
3. Matching finds common traits between user profile and effective job traits
4. Distance calculation uses squared differences of trait values
5. Results filtered by minimum score threshold (0.15) and sorted by similarity

## Development Commands

### Building and Running
```bash
# Build the application
./gradlew clean :backend:app:shadowJar

# Run locally (requires DynamoDB setup)
./gradlew :backend:app:run
```

### Testing
```bash
# Run tests
./gradlew test

# Run specific test
./gradlew :backend:app:test --tests "ClassName.methodName"
```

### Infrastructure Management
```bash
# Deploy infrastructure (from /infrastructure directory)
pulumi up

# Preview changes
pulumi preview

# Destroy infrastructure
pulumi destroy
```

## Important Environment Variables

Required for local development and deployment:
- `JOBS_TABLE_NAME`: DynamoDB jobs table name
- `COMPANIES_TABLE_NAME`: DynamoDB companies table name  
- `USERS_TABLE_NAME`: DynamoDB users table name
- `AWS_REGION`: AWS region for DynamoDB client

## Database Schema Notes

### Users Table
- Primary key: `id` (String)
- GSI: `EmailIndex` on `email` field for email-based user lookup
- Profile traits stored as nested Map in DynamoDB

### Companies/Jobs Tables
- Primary key: `id` (String)
- Jobs reference companies via `companyId` field
- Traits stored as flat key-value maps (String -> Int)

## API Endpoints

Core endpoints in `Routing.kt`:
- `GET /jobs` - List all jobs
- `POST /jobs` - Create job
- `GET /companies` - List companies  
- `POST /companies` - Create company
- `POST /users` - Create user
- `GET /users/{id}` - Get user by ID or email
- `POST /match` - Find matching jobs for profile

## Code Conventions

- Kotlin coroutines for async operations
- Repository pattern for data access
- Dependency injection via constructor parameters
- Kotlinx.serialization for JSON handling
- AWS SDK for Kotlin (not Java SDK)
- Ktor OpenAPI plugin for API documentation