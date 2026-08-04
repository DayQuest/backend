# DayQuest API Documentation

## Overview

DayQuest uses **SpringDoc OpenAPI 3** for API documentation, aggregated centrally at the **API Gateway** (port `8080`).

---

## 🔍 Documentation UIs

| Interface | URL | Description |
|---|---|---|
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` | Interactive — try endpoints directly |
| **ReDoc** | `http://localhost:8080/redoc` | Beautiful read-only reference view |
| **OpenAPI JSON** | `http://localhost:8080/v3/api-docs` | Raw OpenAPI 3.0 spec (gateway-level) |

### Per-Service Specs (via Swagger UI dropdown)

| Service | Display Name | Docs URL |
|---|---|---|
| User & Auth Service | User Service (Auth + Users) | `http://localhost:8080/user-service/v3/api-docs` |
| Quest Service | Quest Service | `http://localhost:8080/quest-service/v3/api-docs` |
| Video Service | Video Service | `http://localhost:8080/video-service/v3/api-docs` |
| Social Service | Social Service (Comments + Friends + Hashtags) | `http://localhost:8080/social-service/v3/api-docs` |
| Content Service | Content Service (Badges + Streaks + Reports) | `http://localhost:8080/content-service/v3/api-docs` |
| Notification Service | Notification Service | `http://localhost:8080/notification-service/v3/api-docs` |

### Direct Service Docs (bypassing gateway, for development)

| Service | Port | API Docs URL |
|---|---|---|
| User/Auth Service | 8081 | `http://localhost:8081/v3/api-docs` |
| Quest Service | 8083 | `http://localhost:8083/v3/api-docs` |
| Notification Service | 8082 | `http://localhost:8082/v3/api-docs` |
| Video Service | 8085 | `http://localhost:8085/v3/api-docs` |
| Social Service | 8086 | `http://localhost:8086/v3/api-docs` |
| Content Service | 8087 | `http://localhost:8087/v3/api-docs` |

---

## 🔐 Authentication

All protected endpoints require a **JWT Bearer token**:

```
Authorization: Bearer <access_token>
```

1. **Register**: `POST /auth/register`
2. **Verify email**: Click link from email (`GET /auth/verify?token=...`)
3. **Login**: `POST /auth/login` → get `accessToken` and `refreshToken`
4. **Use**: Pass `accessToken` as `Authorization: Bearer <token>`
5. **Refresh**: `POST /auth/refresh` when access token expires (TTL: 15 min)

---

## 📋 Endpoints

### Authentication Service (`/auth/**`)

| Method | Path | Description | Auth |
|---|---|---|---|
| `POST` | `/auth/register` | Register a new user | Public |
| `POST` | `/auth/login` | Login — returns JWT tokens | Public |
| `POST` | `/auth/refresh` | Refresh access token | Public |
| `POST` | `/auth/logout` | Logout | Bearer |
| `GET` | `/auth/verify` | Verify email (link from email) | Public |
| `POST` | `/auth/verify` | Verify email (API) | Public |
| `POST` | `/auth/resend-verification` | Resend verification email | Public |
| `POST` | `/auth/forgot-password` | Request password reset email | Public |
| `POST` | `/auth/reset-password` | Reset password with token | Public |
| `GET` | `/auth/reset-password` | Password reset form (HTML) | Public |
| `POST` | `/auth/token/validate` | Validate a JWT token | Public/Internal |

### User Service (`/users/**`)

| Method | Path | Description | Auth |
|---|---|---|---|
| `GET` | `/users/{uuid}` | Get user profile by UUID | Bearer |
| `GET` | `/users/profile/{username}` | Get user profile by username | Bearer |
| `GET` | `/users/search?query=&page=&size=` | Search users by username | Bearer |
| `GET` | `/users/{username}/uuid` | Get UUID by username | Bearer |
| `GET` | `/users/profilepicture/{username}` | Get profile picture (JPEG) | Public |
| `POST` | `/users/setprofilepicture` | Upload profile picture | Bearer |
| `PUT` | `/users/email` | Update email address | Bearer |
| `PUT` | `/users/password` | Update password | Bearer |
| `DELETE` | `/users` | Delete account | Bearer |
| `POST` | `/users/{uuid}/follow` | Follow a user | Bearer |
| `DELETE` | `/users/{uuid}/follow` | Unfollow a user | Bearer |
| `GET` | `/users/{uuid}/followers` | Get followers list | Bearer |
| `GET` | `/users/{uuid}/following` | Get following list | Bearer |
| `GET` | `/users/{uuid}/badges` | Get user's badges | Bearer |
| `GET` | `/users/rerolls` | Get remaining quest rerolls today | Bearer |

### Quest Service (`/quests/**`)

| Method | Path | Description | Auth |
|---|---|---|---|
| `GET` | `/quests` | Get quests (paginated + sortable) | Bearer |
| `GET` | `/quests/{uuid}` | Get quest by UUID | Bearer |
| `POST` | `/quests` | Create a new quest | Bearer |
| `DELETE` | `/quests/{uuid}` | Delete a quest (creator only) | Bearer |
| `POST` | `/quests/{uuid}/like` | Like a quest | Bearer |
| `DELETE` | `/quests/{uuid}/like` | Remove like | Bearer |
| `POST` | `/quests/{uuid}/dislike` | Dislike a quest | Bearer |
| `DELETE` | `/quests/{uuid}/dislike` | Remove dislike | Bearer |
| `GET` | `/quests/daily` | Get today's daily quest | Bearer |
| `POST` | `/quests/daily/reroll` | Reroll daily quest (max 3/day) | Bearer |
| `GET` | `/quests/top` | Get top-rated quests | Bearer |
| `GET` | `/quests/random` | Get a random quest | Bearer |
| `GET` | `/quests/user/{userId}` | Get quests by user | Bearer |

### Video Service (`/videos/**`)

| Method | Path | Description | Auth |
|---|---|---|---|
| `POST` | `/videos/upload` | Upload a video (multipart, max 500MB) | Bearer |
| `GET` | `/videos` | Get videos (filters: userUuid, questUuid, page, size, sort) | Bearer |
| `GET` | `/videos/{uuid}` | Get video by UUID | Bearer |
| `POST` | `/videos/next` | Get next video for feed algorithm | Bearer |
| `POST` | `/videos/{uuid}/vote?isUpvote=true\|false` | Vote on a video | Bearer |
| `POST` | `/videos/{uuid}/view` | Mark video as viewed | Bearer |
| `DELETE` | `/videos/{uuid}` | Delete a video (owner only) | Bearer |

### Social Service

#### Comments (`/comments/**`)

| Method | Path | Description | Auth |
|---|---|---|---|
| `POST` | `/comments` | Create a comment | Bearer |
| `GET` | `/comments/{id}` | Get comment by ID | Bearer |
| `PUT` | `/comments/{id}` | Update a comment (own only) | Bearer |
| `DELETE` | `/comments/{id}` | Delete a comment (own only) | Bearer |
| `GET` | `/comments/video/{videoId}` | Get comments for a video | Bearer |
| `GET` | `/comments/quest/{questId}` | Get comments for a quest | Bearer |
| `GET` | `/comments/count/video/{videoId}` | Get comment count for a video | Bearer |
| `GET` | `/comments/count/quest/{questId}` | Get comment count for a quest | Bearer |

#### Friendships (`/friends/**`)

| Method | Path | Description | Auth |
|---|---|---|---|
| `POST` | `/friends/request/{friendUuid}` | Send a friend request | Bearer |
| `POST` | `/friends/{friendshipId}/accept` | Accept a friend request | Bearer |
| `POST` | `/friends/{friendshipId}/decline` | Decline a friend request | Bearer |
| `DELETE` | `/friends/{friendUuid}` | Remove a friend | Bearer |
| `POST` | `/friends/block/{blockedUuid}` | Block a user | Bearer |
| `GET` | `/friends` | Get friends list (paginated) | Bearer |
| `GET` | `/friends/pending` | Get pending requests (paginated) | Bearer |
| `GET` | `/friends/status/{otherUserUuid}` | Get friendship status | Bearer |
| `GET` | `/friends/count` | Get total friend count | Bearer |

#### Hashtags (`/hashtags/**`)

| Method | Path | Description | Auth |
|---|---|---|---|
| `GET` | `/hashtags/trending` | Get trending hashtags | Bearer |
| `GET` | `/hashtags/search?query=` | Search hashtags | Bearer |
| `GET` | `/hashtags/{name}` | Get hashtag by name | Bearer |
| `POST` | `/hashtags` | Create or get hashtags | Bearer |

### Content Service

#### Badges (`/badges/**`)

| Method | Path | Description | Auth |
|---|---|---|---|
| `GET` | `/badges` | Get all badges | Bearer |
| `GET` | `/badges/{id}` | Get badge by ID | Bearer |
| `GET` | `/badges/me` | Get my badges | Bearer |
| `GET` | `/badges/user/{userUuid}` | Get a user's badges | Bearer |
| `POST` | `/badges` | Create a badge | Admin |
| `POST` | `/badges/{badgeId}/award/{userUuid}` | Award badge to user | Admin |

#### Streaks (`/streaks/**`)

| Method | Path | Description | Auth |
|---|---|---|---|
| `GET` | `/streaks` | Get my current streak | Bearer |
| `GET` | `/streaks/user/{userUuid}` | Get a user's streak | Bearer |
| `POST` | `/streaks/record` | Record daily activity | Bearer |
| `GET` | `/streaks/leaderboard` | Current streak leaderboard | Bearer |
| `GET` | `/streaks/leaderboard/longest` | Longest streak leaderboard | Bearer |

#### Reports (`/reports/**`)

| Method | Path | Description | Auth |
|---|---|---|---|
| `POST` | `/reports` | Submit a content report | Bearer |
| `GET` | `/reports/{id}` | Get report by ID | Admin |
| `GET` | `/reports` | Get all reports (filtered) | Admin |
| `POST` | `/reports/{id}/resolve` | Resolve a report | Admin |
| `GET` | `/reports/count/open` | Get open report count | Admin |

### Notification Service (Internal)

| Method | Path | Description | Auth |
|---|---|---|---|
| `POST` | `/notifications/send` | Send a notification | Internal |

---

## ⚡ Rate Limits

| Endpoint Group | Limit |
|---|---|
| Default | 100 req/s |
| Login (`/auth/login`) | 10 req/min |
| Register (`/auth/register`) | 10 req/min |
| Upload (`/videos/upload`) | 20 req/min |
| Search (`/users/search`) | 60 req/min |

---

## 🛠 Implementation Details

### OpenAPI Annotations used
- `@OpenAPIDefinition` — service-level metadata (title, description, servers, security)
- `@SecurityScheme` — JWT Bearer token definition
- `@Tag` — groups endpoints in the UI
- `@Operation` — summary and description per endpoint
- `@ApiResponses` / `@ApiResponse` — response codes and descriptions
- `@Parameter` — path/query/header parameter documentation

### Files Added/Modified

| File | Change |
|---|---|
| `apigateway/config/SwaggerConfig.java` | Full rewrite with OpenAPI bean, ReDoc page, global security customizer |
| `apigateway/resources/application.properties` | Enhanced Swagger UI config (filter, try-it-out, syntax highlight, monokai theme) |
| `apigateway/config/SecurityConfig.java` | Added `/redoc` to permit list |
| `user-service/pom.xml` | Added `springdoc-openapi-starter-webmvc-ui` dependency |
| `user-service/config/OpenApiConfig.java` | New — service OpenAPI definition |
| `user-service/resources/application.properties` | Added SpringDoc properties |
| `user-service/controllers/AuthController.java` | Full OpenAPI annotation coverage |
| `user-service/controllers/UserController.java` | Full OpenAPI annotation coverage |
| `quest-service/config/OpenApiConfig.java` | New — service OpenAPI definition |
| `quest-service/controllers/QuestController.java` | Full OpenAPI annotation coverage |
| `quest-service/resources/application.properties` | New — basic config with SpringDoc |
| `video-service/config/OpenApiConfig.java` | New — service OpenAPI definition |
| `video-service/controller/VideoController.java` | Added missing @Operation/@ApiResponses |
| `social-service/config/OpenApiConfig.java` | New — service OpenAPI definition |
| `content-service/config/OpenApiConfig.java` | New — service OpenAPI definition |
