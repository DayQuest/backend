# API Documentation

## Overview

This project uses SpringDoc OpenAPI for API documentation.

### Swagger UI

You can access the aggregated API documentation at:

http://localhost:8080/swagger-ui.html

This UI aggregates documentation from all microservices:
- **Auth Service**: http://localhost:8080/swagger-ui/index.html?urls.primaryName=auth
- **User Service**: http://localhost:8080/swagger-ui/index.html?urls.primaryName=user
- **Quest Service**: http://localhost:8080/swagger-ui/index.html?urls.primaryName=quest
- **Notification Service**: http://localhost:8080/swagger-ui/index.html?urls.primaryName=notification
- **Video Service**: http://localhost:8080/swagger-ui/index.html?urls.primaryName=video
- **Social Service**: http://localhost:8080/swagger-ui/index.html?urls.primaryName=social
- **Content Service**: http://localhost:8080/swagger-ui/index.html?urls.primaryName=content

## Endpoints

### Auth Service

| Method | Path | Description | Access |
|---|---|---|---|
| POST | `/auth/register` | Register a new user | Public |
| POST | `/auth/login` | Login and get JWT token | Public |
| POST | `/auth/refresh` | Refresh JWT token | Public |
| POST | `/auth/logout` | Logout (invalidate token) | Authenticated |
| POST | `/auth/verify` | Verify email address | Public |
| POST | `/auth/forgot-password` | Request password reset email | Public |
| POST | `/auth/reset-password` | Reset password using token | Public |
| POST | `/auth/token/validate` | Validate a JWT token | Internal/Public |

### User Service

| Method | Path | Description | Access |
|---|---|---|---|
| GET | `/users/{uuid}` | Get user details by UUID | Authenticated |
| GET | `/users/profile/{username}` | Get public user profile | Authenticated |
| GET | `/users/search` | Search for users | Authenticated |
| GET | `/users/{username}/uuid` | Get UUID from username | Authenticated |
| GET | `/users/profilepicture/{username}` | Get profile picture | Public |
| PUT | `/users/max-xp` | Update max XP | Authenticated |
| PUT | `/users/email` | Update email address | Authenticated |
| PUT | `/users/password` | Update password | Authenticated |
| DELETE | `/users` | Delete account | Authenticated |
| POST | `/users/{uuid}/follow` | Follow a user | Authenticated |
| DELETE | `/users/{uuid}/follow` | Unfollow a user | Authenticated |
| GET | `/users/followers` | Get my followers | Authenticated |
| GET | `/users/following` | Get users I follow | Authenticated |
| POST | `/users/profilepicture` | Upload profile picture | Authenticated |
| GET | `/users/badges` | Get user badges | Authenticated |

### Quest Service

| Method | Path | Description | Access |
|---|---|---|---|
| GET | `/quests` | Get all quests (paginated) | Authenticated |
| GET | `/quests/{uuid}` | Get quest details | Authenticated |
| POST | `/quests/create` | Create a new quest | Authenticated |
| POST | `/quests/like` | Like a quest | Authenticated |
| DELETE | `/quests/like` | Unlike a quest | Authenticated |
| POST | `/quests/dislike` | Dislike a quest | Authenticated |
| DELETE | `/quests/dislike` | Remove dislike | Authenticated |
| GET | `/quests/daily` | Get daily quest | Authenticated |
| POST | `/quests/reroll` | Reroll daily quest | Authenticated |

### Video Service

| Method | Path | Description | Access |
|---|---|---|---|
| POST | `/videos/upload` | Upload a new video | Authenticated |
| GET | `/videos` | Get all videos (paginated) | Authenticated |
| GET | `/videos/{uuid}` | Get video details | Authenticated |
| GET | `/videos/trending` | Get trending videos | Authenticated |
| GET | `/videos/user/{userUuid}` | Get user's videos | Authenticated |
| GET | `/videos/quest/{questUuid}` | Get quest videos | Authenticated |
| POST | `/videos/next` | Get next video for feed | Authenticated |
| POST | `/videos/{uuid}/upvote` | Upvote a video | Authenticated |
| POST | `/videos/{uuid}/downvote` | Downvote a video | Authenticated |
| POST | `/videos/{uuid}/view` | Mark video as viewed | Authenticated |
| DELETE | `/videos/{uuid}` | Delete a video | Authenticated |

### Social Service

| Method | Path | Description | Access |
|---|---|---|---|
| POST | `/comments` | Create a comment | Authenticated |
| GET | `/comments/{id}` | Get comment by ID | Authenticated |
| PUT | `/comments/{id}` | Update a comment | Authenticated |
| DELETE | `/comments/{id}` | Delete a comment | Authenticated |
| GET | `/comments/video/{videoId}` | Get video comments | Authenticated |
| GET | `/comments/quest/{questId}` | Get quest comments | Authenticated |
| POST | `/friends/request/{friendUuid}` | Send friend request | Authenticated |
| POST | `/friends/{id}/accept` | Accept friend request | Authenticated |
| POST | `/friends/{id}/decline` | Decline friend request | Authenticated |
| DELETE | `/friends/{friendUuid}` | Remove friend | Authenticated |
| POST | `/friends/block/{blockedUuid}` | Block user | Authenticated |
| GET | `/friends` | Get friends list | Authenticated |
| GET | `/friends/pending` | Get pending requests | Authenticated |
| GET | `/friends/status/{userUuid}` | Get friendship status | Authenticated |
| GET | `/hashtags/trending` | Get trending hashtags | Authenticated |
| GET | `/hashtags/search` | Search hashtags | Authenticated |
| GET | `/hashtags/{name}` | Get hashtag details | Authenticated |

### Content Service

| Method | Path | Description | Access |
|---|---|---|---|
| POST | `/reports` | Create a report | Authenticated |
| GET | `/reports` | Get all reports | Admin |
| GET | `/reports/{id}` | Get report details | Admin |
| POST | `/reports/{id}/resolve` | Resolve a report | Admin |
| GET | `/badges` | Get all badges | Authenticated |
| GET | `/badges/{id}` | Get badge details | Authenticated |
| GET | `/badges/me` | Get my badges | Authenticated |
| GET | `/badges/user/{userUuid}` | Get user's badges | Authenticated |
| POST | `/badges` | Create a badge | Admin |
| POST | `/badges/{id}/award/{userUuid}` | Award badge to user | Admin |
| GET | `/streaks` | Get my streak | Authenticated |
| GET | `/streaks/user/{userUuid}` | Get user's streak | Authenticated |
| POST | `/streaks/record` | Record daily activity | Authenticated |
| GET | `/streaks/leaderboard` | Get streak leaderboard | Authenticated |
| GET | `/streaks/leaderboard/longest` | Get longest streak leaderboard | Authenticated |

### Notification Service

| Method | Path | Description | Access |
|---|---|---|---|
| POST | `/notifications/send` | Send a notification (Internal) | Internal |

## Development

The internal API docs are available at:

- Auth: `http://localhost:8084/v3/api-docs`
- User: `http://localhost:8081/v3/api-docs`
- Quest: `http://localhost:8083/v3/api-docs`
- Notification: `http://localhost:8082/v3/api-docs`
- Video: `http://localhost:8085/v3/api-docs`
- Social: `http://localhost:8086/v3/api-docs`
- Content: `http://localhost:8087/v3/api-docs`

