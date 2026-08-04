# API Design Guidelines

This document defines the conventions all DayQuest microservices must follow when designing REST APIs.

---

## Base Conventions

### URL Structure

```
/{resource}/{id}/{sub-resource}
```

- Use **plural nouns** for collections: `/users`, `/quests`, `/videos`, `/comments`
- Use **kebab-case** for multi-word resources: `/friend-requests` (not `/friendRequests`)
- Resources are always rooted at the API Gateway (`http://localhost:8080`)
- Service-internal ports are never exposed to clients directly

### HTTP Methods

| Method | Semantics | Example |
|---|---|---|
| `GET` | Read-only, idempotent | `GET /quests/{uuid}` |
| `POST` | Create new resource or trigger action | `POST /quests` |
| `PUT` | Full replacement | `PUT /users/email` |
| `PATCH` | Partial update | `PATCH /users/{uuid}/status` |
| `DELETE` | Remove resource (soft or hard) | `DELETE /videos/{uuid}` |

---

## Authentication & Authorization

### Client → Gateway

All requests (except public endpoints) must include:

```http
Authorization: Bearer <access-token>
```

### Gateway → Downstream Services

The API Gateway validates the JWT and injects user context as headers:

| Header | Value | Example |
|---|---|---|
| `X-User-Id` | User UUID from JWT subject | `550e8400-e29b-41d4-a716-446655440000` |
| `X-Username` | Username from JWT claim | `alice` |
| `X-User-Roles` | Comma-separated roles | `ROLE_USER,ROLE_ADMIN` |

Downstream services **must not** re-validate the JWT. They trust these injected headers.

### Public Endpoints

These do not require `Authorization`:

```
POST /auth/register
POST /auth/login
POST /auth/refresh
POST /auth/verify
POST /auth/resend-verification
POST /auth/forgot-password
POST /auth/reset-password
GET  /users/profilepicture/{username}
GET  /actuator/**
GET  /swagger-ui/**
GET  /v3/api-docs/**
```

### Admin-Only Endpoints

Admin access is checked by reading the `X-User-Roles` header:

```java
if (!roles.contains("ROLE_ADMIN")) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
}
```

> ⚠️ **FIXME**: Move admin guards to Spring Security `@PreAuthorize` annotations when Spring Security is properly configured across all services.

---

## JWT Token Structure

| Field | Description |
|---|---|
| `sub` | User UUID (string) |
| `username` | Username string |
| `roles` | List of role strings |
| `type` | `"access"` or `"refresh"` |
| Algorithm | HS256 |
| Secret | `JWT_SECRET` env var (Base64-encoded, min 64 chars) |

Token lifetimes:

| Token | Expiry |
|---|---|
| Access token | 15 minutes (900,000 ms) |
| Refresh token | 7 days (604,800,000 ms) |

---

## Standard Response Format

All endpoints must return `ApiResponse<T>` from the `common` module:

```json
{
  "success": true,
  "message": "Quest created successfully",
  "data": { ... }
}
```

### Error Response

```json
{
  "success": false,
  "message": "Quest not found",
  "data": null
}
```

### Login Response (exception to standard format)

```json
{
  "success": true,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "alice",
  "message": "Login successful",
  "tokens": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "expiresIn": 900000,
    "tokenType": "Bearer"
  }
}
```

---

## HTTP Status Codes

| Status | When to Use |
|---|---|
| `200 OK` | Successful GET, PUT, PATCH, DELETE |
| `201 Created` | Successful POST that creates a resource |
| `204 No Content` | Successful DELETE with no body |
| `400 Bad Request` | Validation error, malformed request |
| `401 Unauthorized` | Missing or expired JWT |
| `403 Forbidden` | Authenticated but insufficient permissions |
| `404 Not Found` | Resource does not exist |
| `409 Conflict` | Duplicate resource (e.g. username taken) |
| `429 Too Many Requests` | Rate limit exceeded (returned by gateway) |
| `500 Internal Server Error` | Unhandled exception |

---

## Pagination

All list endpoints that may return many items **must** support pagination.

### Query Parameters

| Parameter | Default | Description |
|---|---|---|
| `page` | `0` | Zero-indexed page number |
| `size` | `20` | Items per page (max 100) |
| `sortBy` | resource-specific | Field to sort by |
| `sortDirection` | `DESC` | `ASC` or `DESC` |

### Response with Pagination

Return a `Page<T>` or wrap with pagination metadata:

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 142,
    "totalPages": 8,
    "last": false
  }
}
```

---

## Resource IDs

All resources use **UUID v4** as identifiers (not auto-increment integers):

```java
@Id
@GeneratedValue(strategy = GenerationType.UUID)
private UUID uuid;
```

URL path parameters: `/{uuid}` — never expose sequential integer IDs.

---

## Soft Deletes

Most entities support **soft deletion** (logical delete, not physical):

```sql
deleted BOOLEAN NOT NULL DEFAULT FALSE,
deleted_at TIMESTAMP
```

- `DELETE` endpoints set `deleted = true` and `deleted_at = now()`.
- All queries must filter `WHERE deleted = false`.
- Physical deletion is not performed (data retained for audit).

Exceptions: hard delete on user cascade events (via RabbitMQ `user.deleted` message).

---

## Rate Limiting

Rate limiting is applied at the API Gateway using Redis.

Default limits (configured in `config-server`):

| User Type | Limit | Window |
|---|---|---|
| Anonymous | 20 requests | 60 seconds |
| Authenticated | 100 requests | 60 seconds |
| Premium | 500 requests | 60 seconds |

Exceeded limits return:
```http
HTTP/1.1 429 Too Many Requests
X-RateLimit-Remaining: 0
X-RateLimit-Reset: <epoch-seconds>
```

Reset rate limits in development:
```bash
make reset-ratelimit
```

---

## API Documentation

Every controller must have SpringDoc OpenAPI annotations:

```java
@Tag(name = "Quests", description = "Quest management endpoints")
@RestController
@RequestMapping("/quests")
public class QuestController {

    @Operation(summary = "Get quest by ID", description = "Returns a single quest.")
    @ApiResponse(responseCode = "200", description = "Quest found")
    @ApiResponse(responseCode = "404", description = "Quest not found")
    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<QuestDTO>> getQuest(@PathVariable UUID uuid) {
        ...
    }
}
```

The aggregated Swagger UI is available at:
```
http://localhost:8080/swagger-ui.html
```

Per-service Swagger (direct):
```
http://localhost:<port>/v3/api-docs
```

---

## Cross-Service Communication Patterns

### Synchronous (HTTP via Gateway)

Only the API Gateway routes external traffic. Services **do not** call each other via HTTP in production. Cross-service data is denormalized at write time.

### Asynchronous (RabbitMQ Events)

Use events for side-effects that don't need an immediate response:

| Exchange | Routing Key | Payload | Purpose |
|---|---|---|---|
| `user.exchange` | `user.deleted` | `UserDeletedEvent` | Cascade deletion across services |
| `notification.exchange` | `email.send` | `EmailTemplate` | Send transactional emails |

Event consumers implement `@RabbitListener` and are idempotent (safe to replay).

---

## Naming Conventions

| Element | Convention | Example |
|---|---|---|
| Package | `com.dayquest.<service>` | `com.dayquest.questservice` |
| Controller | `<Resource>Controller` | `QuestController` |
| Service | `<Resource>Service` | `QuestService` |
| Repository | `<Entity>Repository` | `QuestRepository` |
| DTO | `<Resource>DTO` | `QuestDTO` |
| Event | `<Action>Event` | `UserDeletedEvent` |
| Endpoint path | kebab-case | `/daily-quests` |
