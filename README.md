# DayQuest - Microservices Architecture

A social quest and video sharing platform built with Spring Boot microservices.

## Architecture Overview

```
                                    ┌─────────────────┐
                                    │   API Gateway   │
                                    │     (8080)      │
                                    └────────┬────────┘
                                             │
        ┌────────────────────────────────────┼────────────────────────────────────┐
        │                    │               │               │                    │
        ▼                    ▼               ▼               ▼                    ▼
┌───────────────┐  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│ User Service  │  │ Quest Service │  │ Video Service │  │Social Service │  │Content Service│
│    (8081)     │  │    (8083)     │  │    (8085)     │  │    (8086)     │  │    (8087)     │
└───────────────┘  └───────────────┘  └───────────────┘  └───────────────┘  └───────────────┘
        │                    │               │               │                    │
        └────────────────────┴───────────────┴───────────────┴────────────────────┘
                                             │
                    ┌────────────────────────┼────────────────────────┐
                    │                        │                        │
                    ▼                        ▼                        ▼
            ┌───────────────┐       ┌───────────────┐        ┌───────────────┐
            │  PostgreSQL   │       │    RabbitMQ   │        │     MinIO     │
            │   (5432)      │       │    (5672)     │        │    (9000)     │
            └───────────────┘       └───────────────┘        └───────────────┘
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| **API Gateway** | 8080 | Entry point, JWT authentication, routing |
| **User Service** | 8081 | User management, profiles, follows |
| **Notification Service** | 8082 | Email notifications via RabbitMQ |
| **Quest Service** | 8083 | Quest CRUD, daily quests, ratings |
| **Auth Service** | 8084 | Token validation (internal) |
| **Video Service** | 8085 | Video uploads, streaming, ratings |
| **Social Service** | 8086 | Comments, friendships, hashtags |
| **Content Service** | 8087 | Reports, badges, streaks |
| **Eureka Server** | 8761 | Service discovery |
| **Config Server** | 8888 | Centralized configuration |

## Infrastructure

| Service | Port | Description |
|---------|------|-------------|
| PostgreSQL | 5432 | Main database |
| RabbitMQ | 5672, 15672 | Message queue |
| MinIO | 9000, 9001 | Object storage (S3-compatible) |
| Redis | 6379 | Caching |
| PgAdmin | 5050 | Database management UI |

## Quick Start

### Prerequisites

- Docker & Docker Compose
- Java 21 (for local development)
- Maven 3.9+

### Running with Docker

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Rebuild and start
docker-compose up -d --build
```

### Accessing the Application

- **API Gateway:** http://localhost:8080
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Eureka Dashboard:** http://localhost:8761
- **RabbitMQ Management:** http://localhost:15672
- **MinIO Console:** http://localhost:9001
- **PgAdmin:** http://localhost:5050

## API Documentation

See [API_DOCUMENTATION.md](./API_DOCUMENTATION.md) for complete endpoint documentation.

### Authentication

All protected endpoints require a JWT token:

```bash
# Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "password": "pass"}'

# Use token
curl http://localhost:8080/users/me \
  -H "Authorization: Bearer <token>"
```

## Development

### Project Structure

```
dayquest/
├── apigateway/          # API Gateway (routing, auth filter)
├── auth-service/        # Auth validation service
├── common/              # Shared DTOs, utilities
├── config-server/       # Centralized configuration
├── content-service/     # Reports, badges, streaks
├── eureka-server/       # Service discovery
├── notification-service/# Email notifications
├── quest-service/       # Quest management
├── social-service/      # Comments, friends, hashtags
├── user-service/        # User management
├── video-service/       # Video uploads & streaming
├── docker/              # Docker configs
└── docker-compose.yml   # Docker orchestration
```

### Building Locally

```bash
# Build all modules
mvn clean install -DskipTests

# Build specific service
mvn clean package -pl video-service -am
```

### Running Services Locally

Each service can be run independently:

```bash
cd user-service
mvn spring-boot:run
```

## Environment Variables

Copy `stack.env.example` to `stack.env` and configure:

```env
# Database
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=secret

# JWT
JWT_SECRET=your-secret-key

# RabbitMQ
RABBITMQ_USER=admin
RABBITMQ_PASSWORD=secret

# MinIO
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin

# Mail
MAIL_USERNAME=your-email
MAIL_PASSWORD=your-password
```

## Features

### User Management
- Registration & authentication
- Profile management
- Profile pictures
- Follow system

### Quests
- Create and share quests
- Daily quest assignments
- Like/dislike system
- Reroll functionality

### Videos
- Video uploads (MP4, WebM)
- Automatic thumbnail generation
- View tracking
- Upvote/downvote system
- TikTok-style feed

### Social
- Comments on videos & quests
- Friend requests
- Hashtag system
- User blocking

### Gamification
- Daily streaks
- Achievement badges
- Leaderboards

### Moderation
- Content reporting
- Admin review system
- User punishment system

## License

MIT License - see [LICENSE](./LICENSE) for details.

