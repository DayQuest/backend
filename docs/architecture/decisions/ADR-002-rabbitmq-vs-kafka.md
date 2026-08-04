# ADR-002: RabbitMQ as Message Broker (vs. Apache Kafka)

| Field | Value |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-07-05 |
| **Deciders** | DayQuest Engineering Team |

---

## Context

Certain DayQuest operations are inherently asynchronous and should not block the HTTP request path:

- Sending a welcome / verification email after user registration.
- Sending notification emails for social events (comments, friend requests, quest completions).

We needed to choose a message broker to decouple producers (business services) from consumers (notification-service).

### Options Considered

| Criterion | RabbitMQ | Apache Kafka |
|---|---|---|
| Operational complexity | Low — single process, management UI included | High — ZooKeeper / KRaft + brokers + topics |
| Message semantics | Push (queue / fanout / topic exchanges) | Pull (consumer group offset) |
| Message replay | Limited (with plugins) | Native (retained log, configurable retention) |
| Throughput | Tens of thousands msg/s | Millions msg/s |
| Latency | Sub-millisecond | Low ms |
| Spring Boot integration | `spring-amqp` (mature, simple) | `spring-kafka` (more config) |
| Docker footprint | ~256 MB | > 1 GB (JVM + broker) |

---

## Decision

Use **RabbitMQ** (`rabbitmq:3-management` image).

The current DayQuest messaging load is low-volume, transient, and consumer-driven (push model fits).
There is no requirement for event log replay or high-throughput stream processing.
RabbitMQ's management UI (`:15672`) provides easy visibility into queues and message rates during development.

### Integration

- **Producer**: any service publishes to a named exchange using `RabbitTemplate`.
- **Consumer**: `notification-service` declares a queue and binding via `@RabbitListener`.
- Configuration class: `RabbitConfig.java` in `notification-service`.

---

## Consequences

### Positive
- Simple to run, inspect, and debug during local development.
- Small Docker footprint — important for the monorepo compose stack.
- Spring AMQP auto-configuration with minimal boilerplate.

### Negative
- No built-in event sourcing or log replay.
- If DayQuest scales to millions of events per day, a Kafka migration would be required.

### Future
If the platform grows and real-time analytics or event sourcing become requirements, revisit this decision and consider migrating high-volume topics to Kafka while retaining RabbitMQ for transactional notifications.
