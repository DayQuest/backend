# ADR-003: MinIO for Object Storage

| Field | Value |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-07-05 |
| **Deciders** | DayQuest Engineering Team |

---

## Context

DayQuest needs to store large binary objects:

| Asset | Estimated size | Service |
|---|---|---|
| User profile pictures | 50 KB – 5 MB each | user-service |
| Raw (unprocessed) videos | 10 MB – 250 MB each | video-service |
| Processed videos | 5 MB – 100 MB each | video-service |
| Video thumbnails | 50 KB – 500 KB each | video-service |

Storing binary data in PostgreSQL (`BYTEA`) was rejected early as it would bloat the relational DB and make streaming difficult.

### Options Considered

| Criterion | MinIO | AWS S3 | Local filesystem |
|---|---|---|---|
| Self-hosted | ✅ | ❌ (cloud) | ✅ |
| S3-compatible API | ✅ | ✅ (native) | ❌ |
| Horizontal scaling | ✅ (distributed mode) | ✅ | ❌ |
| Docker compose friendly | ✅ | ❌ | ✅ |
| Cost | Free (OSS) | Pay-per-use | Free |
| SDK | Java SDK available | AWS SDK | N/A |

---

## Decision

Use **MinIO** (`minio/minio` image) as the object storage layer.

MinIO exposes an S3-compatible API, meaning a future migration to AWS S3 or another S3-compatible provider requires only a configuration change (endpoint URL + credentials), with zero code changes.

### Buckets

| Bucket | Environment variable | Contents |
|---|---|---|
| `profile-pictures` | `MINIO_PROFILE_PICTURE_BUCKET` | User avatars |
| `raw-videos` | `MINIO_RAW_VIDEOS_BUCKET` | Uploaded videos (pre-processing) |
| `videos` | `MINIO_VIDEOS_BUCKET` | FFmpeg-processed videos |
| `thumbnails` | `MINIO_THUMBNAIL_BUCKET` | Auto-generated video thumbnails |

### Ports

| Port | Purpose |
|---|---|
| 9000 | S3 API (used by services) |
| 9001 | MinIO Console (web UI) |

### Configuration

```properties
minio.endpoint=${MINIO_ENDPOINT:http://localhost:9000}
minio.accessKey=${MINIO_ACCESS_KEY:minioadmin}
minio.secretKey=${MINIO_SECRET_KEY:minioadmin}
minio.profilePictureBucket=${MINIO_PROFILE_PICTURE_BUCKET:profile-pictures}
```

---

## Consequences

### Positive
- No cloud vendor lock-in; S3-compatible API means easy migration.
- Large file upload limit (250 MB) configured at the Spring multipart layer.
- MinIO Console provides a user-friendly UI for bucket management.

### Negative
- Requires persistent Docker volume (`minio_data`) — data is lost if the volume is deleted without backup.
- Not suitable as a CDN; a reverse proxy (nginx/CloudFront) should sit in front for public video delivery in production.

### Production Recommendation
- Use a separate MinIO cluster or switch to cloud S3 for production.
- Place a CDN (CloudFront, Cloudflare) in front of the `videos` and `thumbnails` buckets.
- Enable MinIO bucket versioning for the `raw-videos` bucket to enable reprocessing.
