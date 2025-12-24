# DayQuest Backend

© 2025 DayQuest  
All rights reserved.

This software is **private**. You are **not** allowed to copy, modify, distribute, or use this code in any way except for the following cases:  

### ✅ Allowed:
- Private use (for personal, non-public and non-commercial purposes).
- Contributing to this project.  

### ❌ Not Allowed:
- Redistributing, sharing, or publishing this code in any form.
- Modifying and using this code in other projects without permission.
- Using this software for commercial purposes.  

---

## 🏗️ Architecture

This project supports both **monolithic** and **microservices** architectures.

### Microservices Architecture

The application is split into the following services:

```
┌─────────────────────────────────────────────────────────────────────┐
│                          API Gateway (8080)                         │
│                    (Routing, Authentication, Rate Limiting)         │
└─────────────────────────────────────────────────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        ▼                           ▼                           ▼
┌───────────────┐         ┌───────────────┐         ┌───────────────┐
│ User Service  │         │ Auth Service  │         │ Video Service │
│    (8081)     │◄───────►│    (8082)     │         │    (8083)     │
│               │         │               │         │               │
│ - Profiles    │         │ - Login/JWT   │         │ - Upload      │
│ - Followers   │         │ - 2FA         │         │ - Processing  │
│ - Badges      │         │ - Password    │         │ - Streaming   │
└───────────────┘         └───────────────┘         └───────────────┘
        │                                                   │
        │         ┌───────────────┐         ┌───────────────┤
        │         │ Quest Service │         │ Social Service│
        └────────►│    (8084)     │◄────────┤    (8085)     │
                  │               │         │               │
                  │ - Daily Quest │         │ - Comments    │
                  │ - Ratings     │         │ - Notifications│
                  │ - Rerolls     │         │ - Friends     │
                  └───────────────┘         └───────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                     Infrastructure Services                          │
├─────────────────┬─────────────────┬─────────────────┬───────────────┤
│ Discovery Server│  Config Server  │    PostgreSQL   │    Redis      │
│     (8761)      │     (8888)      │     (5432)      │    (6379)     │
│    (Eureka)     │   (Centralized  │  (Data Store)   │   (Cache)     │
│                 │    Config)      │                 │               │
└─────────────────┴─────────────────┴─────────────────┴───────────────┘
```

### Service Communication

- **Synchronous**: REST APIs via Feign Clients
- **Service Discovery**: Netflix Eureka
- **Configuration**: Spring Cloud Config Server
- **Caching**: Redis for distributed caching
- **Database**: PostgreSQL (separate databases per service)  


### Folder Structure:

```
DayQuest-backend/
├── docker-compose.dev.yml           # Docker Compose for development (monolith)
├── docker-compose.yml               # Docker Compose for production (monolith)
├── docker-compose.microservices.yml # Docker Compose for microservices
├── Dockerfile                       # Dockerfile for monolith
├── Dockerfile.dev                   # Dockerfile for development
├── pom.xml                          # Parent POM for multi-module Maven project
├── init-databases.sql               # Database initialization script
├── src/                             # Original monolithic source code
└── services/                        # Microservices modules
    ├── common/                      # Shared library (DTOs, exceptions, events)
    ├── discovery-server/            # Eureka service discovery
    ├── config-server/               # Centralized configuration
    ├── api-gateway/                 # API Gateway with routing
    ├── user-service/                # User management service
    ├── auth-service/                # Authentication service
    ├── video-service/               # Video upload/processing service
    ├── quest-service/               # Quest management service
    └── social-service/              # Comments, notifications service
```

### Commands:

#### Monolithic Mode (Legacy)

1. To start the development environment:
   ```bash
   make dev
   ```

2. To start the production environment:
   ```bash
   make prod
   ```

#### Microservices Mode

1. To start all microservices:
   ```bash
   make microservices
   ```

2. To stop microservices:
   ```bash
   make microservices-down
   ```

3. To build all services with Maven:
   ```bash
   make build-all
   ```

4. To build a specific service:
   ```bash
   make build-service SERVICE=user-service
   ```

#### Common Commands

1. To stop and remove containers:
   ```bash
   make down
   ```

2. To view logs of running containers:
   ```bash
   make logs
   ```

3. Get a list of all available commands:
   ```bash
   make help
   ```

### API Endpoints

When running in microservices mode, all requests go through the API Gateway on port 8080:

| Service        | Gateway Path         | Direct Port |
|----------------|----------------------|-------------|
| User Service   | `/api/users/**`      | 8081        |
| Auth Service   | `/api/auth/**`       | 8082        |
| Video Service  | `/api/videos/**`     | 8083        |
| Quest Service  | `/api/quests/**`     | 8084        |
| Social Service | `/api/social/**`     | 8085        |

### Environment Variables

Create a `.env` file with the following variables:

```env
# Database
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_password

# JWT
JWT_SECRET=your_jwt_secret_key
JWT_EXPIRATION=86400000

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# MinIO (Video Storage)
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin

# Email (for Auth Service)
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your_email
SPRING_MAIL_PASSWORD=your_app_password
```
---

## Contribution Rules for Feature Development:

### 1. **Production Branch Rules**

The `production` branch is untouchable. You **must never** directly commit or push any changes to this branch. All production changes will be automatically deployed to the server from this branch, so it is critical that this branch remains stable and only reflects production ready code.

### 2. **Development Branch Rules**

The `development` branch is semi touchable. You are allowed to create new feature branches from it, and you can merge them back into the `development` branch through a pull request. Follow these steps for creating a new feature:

1. **Create a New Feature Branch**  
   When starting a new feature, create a new branch from `development`. Naming conventions for branches should be clear and descriptive of the feature. Example:
   ```bash
   git checkout development
   git checkout -b leck-eier-feature
   ```

2. **Work on the Feature**  
   Implement your feature on the new branch. Make sure to frequently commit your changes with meaningful commit messages.

3. **Create a Pull Request**  
   Once your feature is complete, create a pull request to merge your feature branch back into the `development` branch. The PR should be reviewed by another staff for safety.

4. **Merge Back into Development**  
   Once the PR is approved, merge it into the `development` branch. Ensure no direct commits are made to `development` outside of PR merges.

5. **Deploy to Production**  
   Only after the feature is fully tested and verified in the development branch, will it be merged into the `production` branch by an authorized team member, preferably `AgentP`, if possible. The `production` branch will automatically deploy to the server.

### 3. **Do Not Push Directly to Production**

- All changes to the `production` branch must go through the `development` branch via a PR.
