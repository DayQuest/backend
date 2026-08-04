# Testing Strategy

DayQuest uses a layered testing approach: fast unit tests for isolated business logic, and Testcontainers-based integration tests for persistence and messaging.

---

## Test Categories

| Category | Naming Convention | Scope | Speed |
|---|---|---|---|
| Unit Tests | `*Test.java` | Single class, all dependencies mocked | Fast (< 1s each) |
| Integration Tests | `*IT.java` | Real database + broker via Testcontainers | Slow (10–60s each) |

---

## Running Tests

```bash
# All tests (unit + integration)
make test
# or:
./mvnw test -B

# Unit tests only
make test-unit
# or:
./mvnw test -Dtest="**/*Test" -B

# Integration tests only
make test-integration
# or:
./mvnw test -Dtest="**/*IT" -B

# Tests with coverage report (JaCoCo)
make test-coverage
# or:
./mvnw test jacoco:report -B
# Reports are generated at: <module>/target/site/jacoco/index.html

# Tests for a single service module
make test-service SERVICE=user-service
# or:
./mvnw test -pl user-service -am -B
```

---

## Unit Tests

Unit tests use **JUnit 5** and **Mockito** and must:

- Test one class in isolation.
- Mock all external dependencies (`@MockBean`, `@Mock`, `Mockito.mock()`).
- Not start a Spring context (use plain `@ExtendWith(MockitoExtension.class)`).
- Be fast and deterministic.

### Example Structure

```java
@ExtendWith(MockitoExtension.class)
class QuestServiceTest {

    @Mock
    private QuestRepository questRepository;

    @InjectMocks
    private QuestService questService;

    @Test
    void shouldReturnQuestWhenFound() {
        // given
        UUID id = UUID.randomUUID();
        Quest quest = new Quest();
        quest.setUuid(id);
        when(questRepository.findById(id)).thenReturn(Optional.of(quest));

        // when
        Quest result = questService.findById(id);

        // then
        assertThat(result.getUuid()).isEqualTo(id);
    }
}
```

---

## Integration Tests

Integration tests use **Testcontainers** to spin up real infrastructure (PostgreSQL, RabbitMQ) in Docker.

The `@SpringBootTest` context is started once per test class using Testcontainers with `@DynamicPropertySource` to wire container ports into Spring properties.

### Spring Profile

Integration tests run with `SPRING_PROFILES_ACTIVE=test`. This profile should disable:
- External config server imports (`spring.config.import=optional:...`)
- MinIO (use a mock or in-memory stub if needed)

### Example Integration Test

```java
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class UserRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUser() {
        UserData user = new UserData();
        user.setUuid(UUID.randomUUID());
        user.setUsername("testuser");
        // ... set required fields
        userRepository.save(user);

        Optional<UserData> found = userRepository.findByUsername("testuser");
        assertThat(found).isPresent();
    }
}
```

### Services with Integration Tests

| Service | Test Classes |
|---|---|
| user-service | `UserRepositoryIT`, `AuthControllerIT` |
| quest-service | `QuestRepositoryIT`, `QuestServiceIT` |
| video-service | `VideoRepositoryIT` |
| social-service | `CommentRepositoryIT`, `FriendshipRepositoryIT` |
| content-service | `StreakRepositoryIT`, `ReportRepositoryIT` |

---

## Code Coverage

JaCoCo is configured in the root `pom.xml` and runs automatically during the `test` phase.

```bash
# Generate report after running tests
./mvnw jacoco:report -B
```

Reports are at: `<module>/target/site/jacoco/index.html`

Aggregate reports are uploaded to **Codecov** via GitHub Actions on every CI run.

### Coverage Goals

| Metric | Target |
|---|---|
| Line coverage | ≥ 70% |
| Branch coverage | ≥ 60% |
| Service layer | ≥ 80% |

---

## Test Profiles

Each service's `src/test/resources/application-test.properties` should override:

```properties
# Use in-memory/container config
spring.config.import=

# Disable Eureka for tests
eureka.client.enabled=false
spring.cloud.config.enabled=false

# Flyway runs against Testcontainers DB
spring.flyway.enabled=true
spring.jpa.hibernate.ddl-auto=validate
```

---

## Static Analysis

Three quality gates run on every CI build:

| Tool | Config File | What It Checks |
|---|---|---|
| Checkstyle | `checkstyle.xml` | Code style (indentation, naming, javadoc) |
| SpotBugs | `spotbugs-exclude.xml` | Potential bugs (null dereferences, bad patterns) |
| OWASP Dependency Check | — | Known CVEs in dependencies |

```bash
# Run all quality checks locally
./mvnw checkstyle:check spotbugs:check
```

---

## Testing Messaging (RabbitMQ)

For integration tests involving RabbitMQ:

```java
@Container
static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management")
        .withUser("guest", "guest");

@DynamicPropertySource
static void rabbitProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.rabbitmq.host", rabbitmq::getHost);
    registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    registry.add("spring.rabbitmq.username", () -> "guest");
    registry.add("spring.rabbitmq.password", () -> "guest");
}
```

---

## CI Test Pipeline

GitHub Actions runs on every push/PR:

```yaml
- name: Run Unit & Integration Tests
  run: mvn test -B
  env:
    SPRING_PROFILES_ACTIVE: test
```

Tests that require Docker (Testcontainers) work on GitHub Actions because the `ubuntu-latest` runner has Docker pre-installed.
