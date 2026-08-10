# Monitoring & Observability

This document defines DayQuest backend monitoring standards, dashboard usage, and alert response workflow.

## Stack Overview

- **Metrics collection:** Spring Boot Actuator + Micrometer (`/actuator/prometheus`)
- **Scraping & rule evaluation:** Prometheus (`docker/prometheus/prometheus.yml`)
- **Dashboarding:** Grafana (`docker/grafana/dashboards/dayquest-dashboard.json`)
- **Alert rules:** `docker/prometheus/alerts/dayquest-alerts.yml`

## Dashboard

- **Name:** `DayQuest Backend Observability`
- **Sections:**
  - Service Overview
  - API Performance
  - Error Tracking
  - Infrastructure Health
  - Database/Dependencies
- **Filters:**
  - `Environment`
  - `Service`

Use filters first, then inspect:
1. RPS and latency percentiles (p50/p95/p99)
2. 4xx/5xx rates and error ratios
3. CPU/JVM pressure
4. DB pool health and queue backlog

## Metric Catalog (Baseline KPI/SLI)

- **Throughput:** `http_server_requests_seconds_count` (rate)
- **Latency:** `http_server_requests_seconds_bucket` (histogram quantiles p50/p95/p99)
- **Error rate:** `http_server_requests_seconds_count{status=~"4..|5.."}`
- **Resource usage:** `process_cpu_usage`, `jvm_memory_used_bytes`, `jvm_memory_max_bytes`
- **Database pool:** `hikaricp_connections_active`, `hikaricp_connections_pending`, `hikaricp_connections_timeout_total`
- **Queue health:** `rabbitmq_queue_messages_ready`
- **Availability:** `up`

## Labeling Standard

All scrape targets must include:
- `service`
- `environment`

Application-side tags include:
- `application`
- `environment`

## Alert Baseline

Defined in `docker/prometheus/alerts/dayquest-alerts.yml`:

- Service down (critical)
- High 5xx error rate (warning)
- High p95 latency (warning)
- JVM memory pressure (warning)
- DB connection timeouts (warning)
- RabbitMQ backlog (warning)

## Alert Routing

Prometheus rule evaluation is configured by default.

To route alerts to a team channel, connect Prometheus to Alertmanager and configure receivers (Slack/Teams/email/PagerDuty) in deployment-specific infrastructure configuration.

## Extending Monitoring for New Services/Endpoints

When adding a new service or major endpoint:

1. Expose `/actuator/prometheus`
2. Add `management.metrics.tags.application` and `management.metrics.tags.environment`
3. Add Prometheus scrape config with `service` + `environment` labels
4. Verify endpoint traffic appears in dashboard with `service` filter
5. Confirm baseline alert coverage; add service-specific rules if needed
6. Update this document if new metric families or dependencies are introduced

## Implementation Plan Status (Issue #94)

- [x] Inventory current monitoring stack and dashboards
- [x] Define KPI/SLI metric list and naming/tagging baseline
- [x] Implement missing instrumentation in backend services
- [x] Build dashboard v1 with required panel layout and filters
- [x] Configure baseline alert rules
- [ ] Validate metric quality in staging
- [ ] Perform incident simulation / load test validation
- [ ] Finalize alert channel routing in deployment environment
