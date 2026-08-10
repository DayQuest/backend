# Runbook — Incident Response

> This document describes what to do when something goes wrong in production.
> For monitoring context, see [monitoring.md](./monitoring.md).

---

## Quick Reference

| Service | Port | Health URL |
|---|---|---|
| API Gateway | 8080 | `http://localhost:8080/actuator/health` |
| User Service | 8081 | `http://localhost:8081/actuator/health` |
| Notification Service | 8082 | `http://localhost:8082/actuator/health` |
| Quest Service | 8083 | `http://localhost:8083/actuator/health` |
| Video Service | 8085 | `http://localhost:8085/actuator/health` |
| Social Service | 8086 | `http://localhost:8086/actuator/health` |
| Content Service | 8087 | `http://localhost:8087/actuator/health` |
| Eureka | 8761 | `http://localhost:8761/actuator/health` |
| Config Server | 8888 | `http://localhost:8888/actuator/health` |

---

## Triage: First Steps

1. **Check running containers**
   ```bash
   docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
   ```

2. **Check service logs** (replace `<service>` with container name)
   ```bash
   docker logs <service> --tail 100 -f
   # Examples:
   docker logs user-service --tail 200
   docker logs api-gateway --tail 200
   ```

3. **Check Eureka dashboard** — all healthy services should appear green:
   ```
   http://localhost:8761
   ```

4. **Check Grafana dashboards** (dev only):
   ```
   http://localhost:3000
   ```
5. **Check active Prometheus alerts**:
   ```
   http://localhost:9090/alerts
   ```

---

## Scenario: API Gateway returns 503 / No Service Found

**Symptom**: All API calls return `503 Service Unavailable` or `No instances available`.

**Cause**: A downstream service has crashed or deregistered from Eureka.

**Steps**:
1. Open Eureka UI (`http://localhost:8761`) — identify which service is missing.
2. Check the service's logs:
   ```bash
   docker logs <missing-service> --tail 200
   ```
3. Restart the container:
   ```bash
   docker restart <missing-service>
   ```
4. Wait ~30 seconds for the service to re-register with Eureka.
5. Verify it appears in Eureka and retry the request.

---

## Scenario: Database Connection Failures

**Symptom**: Services log `HikariPool-1 - Connection is not available` or Flyway migration errors.

**Steps**:
1. Verify PostgreSQL is running:
   ```bash
   docker ps | grep postgres
   docker logs postgres_container --tail 50
   ```
2. Check PostgreSQL health:
   ```bash
   docker exec postgres_container pg_isready -U $SPRING_DATASOURCE_USERNAME -d dayquest
   ```
3. If PostgreSQL is down, restart it:
   ```bash
   docker restart postgres_container
   ```
4. After PostgreSQL recovers, restart affected services (they will retry HikariCP connections):
   ```bash
   docker restart user-service quest-service video-service social-service content-service
   ```

---

## Scenario: Flyway Migration Failed on Startup

**Symptom**: Service fails to start with `FlywayException: Found non-empty schema ... without schema history table`.

**Root cause**: A new migration script was added but the database schema is inconsistent.

**Steps**:
1. Read the full error from the service log:
   ```bash
   docker logs <service> 2>&1 | grep -A 20 "FlywayException"
   ```
2. Connect to the database via pgAdmin (`http://localhost:5050`) or psql:
   ```bash
   docker exec -it postgres_container psql -U $SPRING_DATASOURCE_USERNAME -d dayquest
   ```
3. Check the Flyway history table for the affected schema:
   ```sql
   SELECT * FROM <schema>.flyway_schema_history ORDER BY installed_rank DESC;
   ```
4. If the latest migration is marked `FAILED`, remove the failed entry and fix the SQL script:
   ```sql
   DELETE FROM <schema>.flyway_schema_history WHERE success = false;
   ```
5. Restart the service.

> **⚠ Never manually edit `flyway_schema_history` in production without a backup.**

---

## Scenario: RabbitMQ Queue Backlog / Emails Not Sent

**Symptom**: Users report not receiving verification/notification emails. RabbitMQ Management shows messages accumulating.

**Steps**:
1. Open RabbitMQ Management UI:
   ```
   http://localhost:15672
   # Credentials: RABBITMQ_USER / RABBITMQ_PASSWORD (from stack.env)
   ```
2. Check queue depths under **Queues** tab.
3. Check `notification-service` logs for SMTP errors:
   ```bash
   docker logs notification-service --tail 200
   ```
4. Common causes:
   - SMTP credentials invalid → update `MAIL_USERNAME` / `MAIL_PASSWORD` in `stack.env` and restart.
   - Notification service is down → `docker restart notification-service`.
5. If the queue has dead-lettered messages, inspect and requeueue them from the Management UI.

---

## Scenario: MinIO Unreachable / File Uploads Failing

**Symptom**: Profile picture upload or video upload returns 500. Logs show `MinIO connection refused`.

**Steps**:
1. Check MinIO container:
   ```bash
   docker ps | grep minio
   docker logs minio --tail 50
   ```
2. Restart if down:
   ```bash
   docker restart minio
   ```
3. Verify buckets exist via MinIO Console (`http://localhost:9001`).
   - Default buckets: `profile-pictures`, `raw-videos`, `videos`, `thumbnails`.
   - If missing, create them manually or let the service auto-create on next startup.

---

## Scenario: Redis Down / Rate Limiting Broken

**Symptom**: API Gateway logs `Cannot connect to Redis`. Rate limiting stops working (all requests pass through or all are rejected).

**Steps**:
1. Restart Redis:
   ```bash
   docker restart redis_container
   ```
2. Verify connection:
   ```bash
   docker exec -it redis_container redis-cli ping
   # Expected: PONG
   ```
3. To manually clear all rate limit keys:
   ```bash
   make reset-ratelimit
   # or:
   docker exec redis_container redis-cli KEYS "ratelimit:*" | xargs -I {} docker exec redis_container redis-cli DEL {}
   ```

---

## Escalation

| Issue | Owner |
|---|---|
| Database corruption | DBA / backend lead |
| Infrastructure failure | DevOps |
| Application bug | Service owning team |
| Security incident | Security team + engineering lead |

---

## Alert Response Baseline

1. Open Prometheus alerts page and identify firing alert(s): `http://localhost:9090/alerts`
2. In Grafana, apply `Environment` and `Service` filters matching the alert labels
3. Correlate:
   - `DayQuestHighErrorRate` / `DayQuestHighP95Latency` → API Performance + Error Tracking sections
   - `DayQuestJvmMemoryPressure` → Infrastructure Health section
   - `DayQuestDbConnectionTimeouts` / `DayQuestRabbitMQQueueBacklog` → Database/Dependencies section
4. Capture timestamp, impacted service, and likely dependency in incident notes
5. Escalate using the ownership table above if recovery is not immediate
