# Changelog

All notable changes to DayQuest will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

> **Automated generation**: This file can be auto-generated using
> [`git-cliff`](https://git-cliff.org/) with Conventional Commits or
> [`conventional-changelog-cli`](https://github.com/conventional-changelog/conventional-changelog).
>
> ```bash
> # Generate with git-cliff
> git cliff --output CHANGELOG.md
>
> # Or with conventional-changelog
> npx conventional-changelog-cli -p angular -i CHANGELOG.md -s
> ```

---

## [Unreleased]

### Added
- Flyway database migration support for all microservices
- Integration tests with Testcontainers for user-service, quest-service, video-service, social-service, content-service
- Redis-based distributed rate limiting via API Gateway
- Resilience4j circuit breakers and retry policies on inter-service calls
- Prometheus + Grafana monitoring stack (dev compose profile)
- `docs/` folder with architecture, operations, and development documentation

### Changed
- Database schema management migrated from `ddl-auto=update` to Flyway-managed migrations
- HikariCP connection pool tuned per service (pool size, idle timeout, max lifetime)

### Fixed
- N+A

---

## [0.1.0] - 2026-07-05

### Added
- Initial microservices architecture with Spring Boot 3.5.3 and Java 21
- API Gateway (port 8080) with Spring Cloud Gateway MVC and Eureka load balancing
- Eureka Service Discovery (port 8761)
- Spring Cloud Config Server (port 8888)
- **User Service** (port 8081): registration, authentication, JWT tokens, profile pictures via MinIO, follow system
- **Auth endpoints** (port 8084 internally): login, register, refresh, logout, email verification, password reset
- **Quest Service** (port 8083): quest CRUD, daily quest assignment, like/dislike, reroll system
- **Video Service** (port 8085): video upload, FFmpeg thumbnail generation, view tracking, upvote/downvote, TikTok-style feed
- **Social Service** (port 8086): comments (threaded), friend requests, hashtag system, user blocking
- **Content Service** (port 8087): badge management, content reports (admin), daily streaks, leaderboards
- **Notification Service** (port 8082): asynchronous email delivery via RabbitMQ + ZeptoMail SMTP
- PostgreSQL as shared database with per-service schemas
- RabbitMQ for asynchronous inter-service messaging
- MinIO for object storage (profile pictures, raw videos, processed videos, thumbnails)
- Redis for session/rate-limit caching
- pgAdmin (port 5050) and RabbitMQ Management UI (port 15672)
- Docker Compose files for production (`docker-compose.yml`) and development (`docker-compose.dev.yml`)
- `Makefile` with targets: `dev`, `prod`, `down`, `build`, `logs`, `test`, `test-unit`, `test-integration`, `test-coverage`
- GitHub Actions CI/CD pipeline: build → test → JaCoCo coverage → Docker push
- Qodana static analysis workflow
- Checkstyle + SpotBugs + OWASP Dependency Check Maven plugins
- Aggregated Swagger UI at `http://localhost:8080/swagger-ui.html`
- `common` shared library module (DTOs, utilities)

---

[Unreleased]: https://github.com/your-org/dayquest/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/your-org/dayquest/releases/tag/v0.1.0
