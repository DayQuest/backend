# Local Development Setup

This guide walks you through setting up DayQuest locally for development.

---

## Prerequisites

| Tool | Version | Install |
|---|---|---|
| Java (Temurin) | 21 | https://adoptium.net |
| Maven | 3.8+ | via `mvnw` wrapper (included) |
| Docker Desktop | 24.x | https://www.docker.com/products/docker-desktop |
| Git | Any | https://git-scm.com |
| Make | 3.x | via [GnuWin32](https://gnuwin32.sourceforge.net/packages/make.htm) on Windows |

**Recommended IDE:** IntelliJ IDEA (`.idea/` and `.run/` configs are included in the repo).

---

## 1. Clone & Configure Environment

```bash
git clone https://github.com/your-org/dayquest.git
cd dayquest

# Copy env files
cp .env.example .env
cp stack.env.example stack.env
```

Edit `stack.env` with your values. Minimum required:

```env
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=secret

JWT_SECRET=<at-least-64-random-hex-chars>

RABBITMQ_USER=admin
RABBITMQ_PASSWORD=secret

MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin

MAIL_USERNAME=your@email.com
MAIL_PASSWORD=your-smtp-password

GRAFANA_PASSWORD=admin
REDIS_PASSWORD=secret
```

> **Tip:** Generate a secure JWT secret with:
> ```bash
> openssl rand -hex 64
> ```

---

## 2. Start Infrastructure Services

Run only the infrastructure (no application services) for local Java development:

```bash
# Starts: PostgreSQL, pgAdmin, RabbitMQ, MinIO, Redis
docker-compose --env-file stack.env -f docker-compose.dev.yml up -d postgres pgadmin rabbitmq minio redis
```

Or start the full stack including all services:

```bash
make dev
```

Wait for PostgreSQL to be healthy:
```bash
docker ps | grep postgres
# Status should show "healthy"
```

---

## 3. Build the Project

```bash
# Build all modules (skip tests for speed on first run)
./mvnw clean install -DskipTests

# On Windows:
mvnw.cmd clean install -DskipTests
```

This installs the `common` module to your local Maven repository, which all services depend on.

---

## 4. Run a Service Locally

Each service is an independent Spring Boot application. Use IntelliJ run configs from `.run/` or Maven:

```bash
# Run user-service (make sure PostgreSQL + Eureka are running)
cd user-service
../mvnw spring-boot:run
```

### Service Start-up Dependencies

Before starting a business service, ensure these are running:

| Dependency | How to start |
|---|---|
| PostgreSQL | `docker start postgres_container` |
| Eureka Server | `cd eureka-server && ../mvnw spring-boot:run` |
| Config Server | `cd config-server && ../mvnw spring-boot:run` (optional — services fall back gracefully) |

### Recommended Local Start Order

```
1. PostgreSQL (Docker)
2. RabbitMQ (Docker)
3. Redis (Docker)
4. MinIO (Docker)
5. Eureka Server (Java)
6. Config Server (Java)      ← optional
7. Business Services (Java)  ← any order
8. API Gateway (Java)
```

---

## 5. Verify Setup

```bash
# Eureka UI — check all services are registered
open http://localhost:8761

# Swagger UI — aggregated API docs
open http://localhost:8080/swagger-ui.html

# pgAdmin — database browser
open http://localhost:5050
# Login: PGADMIN_EMAIL / SPRING_DATASOURCE_PASSWORD from stack.env

# MinIO Console
open http://localhost:9001
# Login: MINIO_ACCESS_KEY / MINIO_SECRET_KEY from stack.env

# RabbitMQ Management
open http://localhost:15672
# Login: RABBITMQ_USER / RABBITMQ_PASSWORD from stack.env
```

---

## 6. Running Tests

```bash
# All tests (unit + integration via Testcontainers)
make test

# Unit tests only (classes matching **/*Test)
make test-unit

# Integration tests only (classes matching **/*IT)
make test-integration

# Tests with JaCoCo coverage report
make test-coverage
# Report: target/site/jacoco/index.html in each module

# Tests for a specific service
make test-service SERVICE=user-service
```

> **Note:** Integration tests (`*IT`) use [Testcontainers](https://testcontainers.com/) and spin up
> real Docker containers (PostgreSQL, RabbitMQ) — Docker must be running.

---

## 7. Code Quality Checks

```bash
# Checkstyle
./mvnw checkstyle:check

# SpotBugs
./mvnw spotbugs:check

# OWASP Dependency Vulnerability Check
./mvnw dependency-check:check
```

Checkstyle rules: `checkstyle.xml`
SpotBugs exclusions: `spotbugs-exclude.xml`

---

## 8. Adding a New Service

1. Create a new Maven module directory (e.g. `my-service/`).
2. Add `<module>my-service</module>` to the root `pom.xml`.
3. In the new module's `pom.xml`, inherit from the parent:
   ```xml
   <parent>
       <groupId>com.dayquest</groupId>
       <artifactId>dayquest-parent</artifactId>
       <version>0.0.1-SNAPSHOT</version>
   </parent>
   ```
4. Add a `Dockerfile` (copy from another service).
5. Add the service to `docker-compose.yml` and `docker-compose.dev.yml`.
6. Register a route in `GatewayRoutingConfig.java`.
7. Add the service's actuator to `./docker/prometheus/prometheus.yml` for monitoring.
8. Document it in `docs/architecture/overview.md`.

---

## 9. Useful Commands

```bash
# Tail logs of a specific service
docker logs user-service -f --tail 100

# Open Redis CLI
make redis-cli

# Reset all rate limits in Redis
make reset-ratelimit

# Clean up everything (containers + volumes)
make clean
```
