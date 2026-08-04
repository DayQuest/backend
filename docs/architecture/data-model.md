# Data Model

> All services share a single PostgreSQL instance (`dayquest` database) but each service owns its own **schema**, enforced by Flyway and Hibernate's `default_schema` property.

---

## Schema Map

```
dayquest (database)
├── user_schema      → user-service
├── quest_schema     → quest-service
├── video_schema     → video-service
├── social_schema    → social-service
└── content_schema   → content-service
```

> **Note**: Cross-schema foreign keys do not exist. Services reference entities in other schemas only by UUID, preserving loose coupling. Referential integrity across service boundaries is enforced at the application level.

---

## user_schema

Managed by: `user-service` · Migration: `V1__init.sql`

```mermaid
erDiagram
    user_data {
        UUID uuid PK
        TIMESTAMP created_at
        VARCHAR username UK
        VARCHAR email
        VARCHAR password
        VARCHAR punishment
        INT interactions
        INT left_rerolls
        TIMESTAMP last_reroll
        VARCHAR password_reset_token
        TIMESTAMP password_reset_token_expiry
        VARCHAR verification_code
        TIMESTAMP verification_expiration
        BOOLEAN enabled
        INT followers
        VARCHAR profile_picture_url
        VARCHAR admin_comment
        TIMESTAMP last_login
    }

    user_authorities {
        UUID user_id FK
        VARCHAR authority
    }

    user_badge {
        UUID user_id FK
        UUID badges
    }

    user_done_quest {
        UUID user_id FK
        UUID done_quests
    }

    follow {
        UUID user_id FK
        UUID followed_id FK
        TIMESTAMP timestamp
    }

    badge {
        UUID id PK
        VARCHAR name
        VARCHAR description
        BYTEA image
    }

    badge_user_ids {
        UUID badge_id FK
        UUID user_id
    }

    user_data ||--o{ user_authorities : "has"
    user_data ||--o{ user_badge : "owns"
    user_data ||--o{ user_done_quest : "completed"
    user_data ||--o{ follow : "follows"
    user_data ||--o{ follow : "followed by"
    badge ||--o{ badge_user_ids : "awarded to"
```

### Key Tables

| Table | Purpose |
|---|---|
| `user_data` | Core user entity — credentials, profile, punishment status |
| `user_authorities` | Roles/authorities (e.g. `ROLE_USER`, `ROLE_ADMIN`) |
| `follow` | Bi-directional follow graph (composite PK) |
| `badge` | Badge definitions (stored in user-service for assignment) |
| `user_badge` | Many-to-many: users ↔ badges |
| `user_done_quest` | Tracks which quests a user has completed |

---

## quest_schema

Managed by: `quest-service` · Migration: `V1__init.sql`

```mermaid
erDiagram
    quest {
        UUID uuid PK
        UUID creator_uuid
        VARCHAR creator_username
        VARCHAR title
        VARCHAR description
        INT likes
        INT dislikes
        INT score
        INT video_count
        TIMESTAMP created_at
        BOOLEAN active
        BOOLEAN deleted
        TIMESTAMP deleted_at
    }

    daily_quest_assignment {
        UUID id PK
        UUID user_id
        UUID quest_id FK
        DATE assigned_date
        INT rerolls_used
        INT max_rerolls
        BOOLEAN completed
        TIMESTAMP completed_at
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    quest_rating {
        UUID id PK
        UUID user_id
        UUID quest_id FK
        BOOLEAN is_like
        TIMESTAMP created_at
    }

    quest ||--o{ daily_quest_assignment : "assigned as"
    quest ||--o{ quest_rating : "rated by"
```

### Key Tables

| Table | Purpose |
|---|---|
| `quest` | Quest entity with scoring and soft-delete |
| `daily_quest_assignment` | One quest per user per day (unique constraint `user_id, assigned_date`) |
| `quest_rating` | Like/dislike per user per quest (unique constraint) |

### Indexes

- `idx_daily_quest_user_date` — primary lookup for today's quest
- `idx_quest_rating_like` — filtered ratings for like/dislike counts

---

## video_schema

Managed by: `video-service` · Migration: `V1__init.sql`

```mermaid
erDiagram
    videos {
        UUID uuid PK
        BIGINT version
        VARCHAR title
        VARCHAR description
        VARCHAR file_path
        VARCHAR thumbnail_path
        UUID user_uuid
        UUID quest_uuid
        INT up_votes
        INT down_votes
        INT views
        INT comments
        INT score
        VARCHAR uploader_username
        VARCHAR quest_title
        BOOLEAN deleted
        TIMESTAMP deleted_at
        VARCHAR status
        VARCHAR security_level
        REAL duration
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    video_ratings {
        UUID user_uuid PK
        UUID video_uuid PK
        BOOLEAN is_upvote
    }

    viewed_videos {
        UUID user_uuid PK
        UUID video_uuid PK
    }

    videos ||--o{ video_ratings : "rated by"
    videos ||--o{ viewed_videos : "viewed by"
```

### Key Tables

| Table | Purpose |
|---|---|
| `videos` | Video entity — file paths reference MinIO object keys |
| `video_ratings` | Upvote/downvote per user (composite PK prevents duplicates) |
| `viewed_videos` | View deduplication per user |

### Notable Fields

- `status` — processing state (`PENDING`, `PROCESSING`, `READY`, `FAILED`)
- `security_level` — visibility (`PUBLIC`, `PRIVATE`, etc.)
- `file_path` / `thumbnail_path` — MinIO object keys, not full URLs
- `score` — computed ranking score for feed ordering

---

## social_schema

Managed by: `social-service` · Migration: `V1__init.sql`

```mermaid
erDiagram
    comments {
        UUID id PK
        BIGINT version
        VARCHAR content
        UUID entity_id
        VARCHAR entity_type
        UUID user_uuid
        VARCHAR username
        TIMESTAMP created_at
        TIMESTAMP updated_at
        UUID parent_comment_id FK
        INT likes
        INT depth
        INT reply_count
        BOOLEAN deleted
        TIMESTAMP deleted_at
    }

    friendships {
        UUID id PK
        BIGINT version
        UUID user_uuid
        UUID friend_uuid
        VARCHAR status
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    hashtags {
        UUID id PK
        VARCHAR name UK
        INT usage_count
        TIMESTAMP created_at
        TIMESTAMP last_used_at
    }

    entity_hashtags {
        UUID entity_id PK
        UUID hashtag_id PK
        VARCHAR entity_type
    }

    comments ||--o{ comments : "replies to"
    hashtags ||--o{ entity_hashtags : "tagged on"
```

### Key Tables

| Table | Purpose |
|---|---|
| `comments` | Polymorphic comments on videos or quests (`entity_id` + `entity_type`) |
| `friendships` | Friend request state machine (`PENDING`, `ACCEPTED`, `BLOCKED`) |
| `hashtags` | Global hashtag registry with usage counts |
| `entity_hashtags` | Polymorphic tag assignment (video, quest, etc.) |

---

## content_schema

Managed by: `content-service` · Migration: `V1__init.sql`

```mermaid
erDiagram
    badges {
        UUID id PK
        VARCHAR name UK
        VARCHAR description
        VARCHAR icon_url
    }

    badge_users {
        UUID badge_id FK
        UUID user_uuid
    }

    reports {
        UUID id PK
        BIGINT version
        VARCHAR description
        UUID entity_id
        UUID reporter_uuid
        VARCHAR type
        VARCHAR status
        VARCHAR reason
        TIMESTAMP created_at
        TIMESTAMP resolved_at
        UUID resolved_by_uuid
        VARCHAR mod_message
    }

    streaks {
        UUID id PK
        UUID user_uuid UK
        INT current_streak
        INT longest_streak
        TIMESTAMP last_activity_date
        TIMESTAMP created_at
    }

    badges ||--o{ badge_users : "awarded to"
```

### Key Tables

| Table | Purpose |
|---|---|
| `badges` | Badge definitions (canonical source) |
| `badge_users` | Which users have been awarded which badge |
| `reports` | Content moderation reports (`OPEN` → `RESOLVED`) |
| `streaks` | Per-user daily activity streak tracking |

---

## Cross-Service Data Flow

```mermaid
flowchart LR
    US[user-service\nuser_schema] -- UUID ref --> QS[quest-service\nquest_schema]
    US -- UUID ref --> VS[video-service\nvideo_schema]
    US -- UUID ref --> SS[social-service\nsocial_schema]
    US -- UUID ref --> CS[content-service\ncontent_schema]
    QS -- UUID ref --> VS
```

> Services never query each other's databases directly. Cross-boundary data (e.g. `uploader_username` on `videos`, `creator_username` on `quest`) is **denormalized** at write time to avoid runtime inter-service calls for read-heavy operations.
