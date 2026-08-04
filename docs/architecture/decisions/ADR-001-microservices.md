# ADR-001: Microservices Architecture

| Field | Value |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-07-05 |
| **Deciders** | DayQuest Engineering Team |

---

## Context

DayQuest is a social platform combining daily quests, short-form video sharing, gamification (streaks/badges), and social features (comments, friends, hashtags).


Key forces at play:
- **Independent deployability**: Video processing (FFmpeg) has very different resource requirements from, say, the notification sender.
- **Technology heterogeneity**: Future flexibility to use different runtimes or databases per domain.
- **Developer experience**: Multiple developers should be able to work on different features in isolation.
- **Operational complexity**: A young team must be able to deploy and debug the system without a large DevOps investment.

---

## Decision

We adopt a **microservices architecture** where each bounded domain is an independently deployable Spring Boot service, orchestrated via Docker Compose and communicating through:

1. **Synchronous HTTP** via the API Gateway (Spring Cloud Gateway MVC) with Eureka client-side load balancing.
2. **Asynchronous messaging** via RabbitMQ for fire-and-forget events (e.g. sending notification emails after registration).

All services are maintained in a **single Git monorepo** with a Maven multi-module parent POM for shared dependency management.

### Services

| Service | Domain |
|---|---|
| user-service | Identity, authentication, profiles, follow |
| quest-service | Quest lifecycle, daily assignment, ratings |
| video-service | Upload, processing, feed |
| social-service | Comments, friends, hashtags |
| content-service | Badges, moderation, streaks |
| notification-service | Email delivery |
| apigateway | Edge proxy, rate limiting |
| eureka-server | Service registry |
| config-server | Centralised configuration |

---

## Consequences

### Positive
- Services can be scaled independently (video-service for heavy I/O, quest-service is lightweight).
- Clear domain boundaries reduce cognitive load per PR.
- CI pipeline builds and tests all services in one pass while Docker images are pushed per service.

### Negative
- Distributed system introduces network latency and the need for resilience patterns (circuit breakers, retries).
- Integration testing requires Testcontainers or a full compose stack.
- Operational overhead: 9 containers must be healthy before the system works end-to-end.

### Mitigations
- Resilience4j circuit breakers + retry policies added to inter-service calls.
- Testcontainers used in `*IT` integration test classes so tests are fully self-contained.
- `Makefile` targets (`make dev`, `make test`) simplify the developer workflow.
