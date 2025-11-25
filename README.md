# Loan Application Service

## Overview

A **GenAI-Native Factory** reference implementation demonstrating ADR145 architecture patterns using:

- **Quarkus** (native-first framework with build-time DI)
- **Temporal.io** (workflow orchestration)
- **Kafka** (event publishing for data contracts)
- **PostgreSQL** (reactive data access via Vert.x Reactive PG Client)
- **OpenTelemetry** (tracing-first observability)
- **Gradle** (build tool)

### Architecture Pattern: "Thin Triggers, Smart Workflows, Dumb Activities"

```
┌─────────────────────────────────────────────────────────────────┐
│                        THIN TRIGGERS                            │
│  REST Endpoints & Kafka Consumers                               │
│  - Validate input                                               │
│  - Add business IDs to trace context                            │
│  - Call WorkflowClient.start()                                  │
│  - NO business logic!                                           │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                     SMART WORKFLOWS                             │
│  Temporal Workflows                                             │
│  - ALL business logic lives here                                │
│  - Deterministic (no I/O, no randomness)                        │
│  - Imperative (clear, step-by-step)                             │
│  - Testable (TestWorkflowEnvironment)                           │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                     DUMB ACTIVITIES                             │
│  Temporal Activities                                            │
│  - Simple I/O wrappers                                          │
│  - NO business logic!                                           │
│  - Call repositories, external APIs, Kafka                      │
└─────────────────────────────────────────────────────────────────┘
```

## Project Structure

```
loan-application-service/
├── build.gradle.kts                            # Main build file
├── settings.gradle.kts                         # Settings
├── gradle.properties                           # Gradle properties
├── gradle/
│   └── libs.versions.toml                      # Version catalog
├── src/
│   ├── main/
│   │   ├── java/com/addi/loan/
│   │   │   ├── infrastructure/                 # Platform infrastructure
│   │   │   │   ├── config/                     # Temporal, OTEL config
│   │   │   │   ├── kafka/                      # Event publisher
│   │   │   │   ├── logging/                    # Structured logger
│   │   │   │   └── persistence/                # Reactive data source
│   │   │   │
│   │   │   └── features/                       # VSA: One folder per feature
│   │   │       ├── submitloan/                 # Feature: Submit Loan
│   │   │       │   ├── SubmitLoanResource.java         # REST thin trigger
│   │   │       │   ├── SubmitLoanWorkflow.java         # Workflow interface
│   │   │       │   ├── SubmitLoanWorkflowImpl.java     # Smart workflow
│   │   │       │   ├── SubmitLoanActivities.java       # Activities interface
│   │   │       │   ├── SubmitLoanActivitiesImpl.java   # Dumb activities
│   │   │       │   ├── LoanApplicationEntity.java      # Entity
│   │   │       │   ├── LoanApplicationRepository.java  # Reactive repo
│   │   │       │   └── dto/                            # DTOs
│   │   │       │
│   │   │       ├── checkcredit/                # Feature: Credit Check
│   │   │       │   └── ...                             # Child workflow
│   │   │       │
│   │   │       └── loanevent/                  # Feature: Kafka Consumer
│   │   │           └── LoanEventConsumer.java          # Kafka thin trigger
│   │   │
│   │   └── resources/
│   │       ├── application.yaml                # Quarkus config
│   │       └── db/migration/                   # Flyway migrations
│   │
│   └── test/
│       └── java/com/addi/loan/
│           ├── features/submitloan/
│           │   ├── SubmitLoanWorkflowTest.java         # Unit test
│           │   └── SubmitLoanResourceIT.java           # Integration test
│           └── ArchitectureTest.java                   # ArchUnit rules
│
└── docker-compose.yaml                         # Local dev environment
```

## Prerequisites

- **JDK 21** (Temurin recommended)
- **Docker & Docker Compose** (for local infrastructure)
- **Gradle 8+** (wrapper included)

## Quick Start

### 1. Start Infrastructure

```bash
# Start PostgreSQL, Temporal, Kafka, Jaeger
docker-compose up -d

# Verify services are running
docker-compose ps
```

**Services:**
- PostgreSQL: `localhost:5432`
- Temporal Server: `localhost:7233`
- Temporal UI: `http://localhost:8080`
- Kafka (Redpanda): `localhost:9092`
- Kafka UI: `http://localhost:8081`
- Jaeger UI: `http://localhost:16686`

### 2. Run in Development Mode

```bash
# Start with hot reload
./gradlew quarkusDev
```

The application will start on `http://localhost:8080` (note: this conflicts with Temporal UI, so we'll change the app port).

**Update `application.yaml` to use port 8090:**

```yaml
quarkus:
  http:
    port: 8090
```

Then restart:

```bash
./gradlew quarkusDev
```

### 3. Submit a Loan Application

```bash
curl -X POST http://localhost:8090/api/v1/loans \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "client-123",
    "amount": 25000,
    "termMonths": 36,
    "purpose": "Home improvement"
  }'
```

**Response:**

```json
{
  "loanId": "loan-abc-123",
  "status": "PROCESSING",
  "message": "Loan application submitted"
}
```

### 4. Check Loan Status

```bash
curl http://localhost:8090/api/v1/loans/loan-abc-123
```

### 5. View in Temporal UI

Open `http://localhost:8080` and navigate to Workflows to see the loan processing workflow.

### 6. View Traces in Jaeger

Open `http://localhost:16686` to see distributed traces with business IDs (`client.id`, `loan.id`).

## Build & Test

### Run Tests

```bash
# All tests (unit + integration)
./gradlew test

# Only unit tests
./gradlew test --tests '*Test'

# Only integration tests
./gradlew test --tests '*IT'

# ArchUnit tests (enforce ADR145 patterns)
./gradlew test --tests 'ArchitectureTest'
```

### Build JAR

```bash
./gradlew build
```

### Build Native Image

```bash
# Build native executable (requires Docker)
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true

# Run native executable
./build/loan-application-service-1.0.0-SNAPSHOT-runner
```

### Build Docker Image

```bash
# JVM-based image (faster build)
./gradlew build
docker build -f src/main/docker/Dockerfile.jvm -t loan-service:jvm .

# Native image (smaller runtime, slower build)
docker build -f src/main/docker/Dockerfile.native -t loan-service:native .
```

### Run with Docker

```bash
docker run -p 8090:8080 \
  -e DB_HOST=host.docker.internal \
  -e TEMPORAL_ADDRESS=host.docker.internal:7233 \
  -e KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  loan-service:jvm
```

## Testing Scenarios

### Scenario 1: Auto-Approve (Excellent Credit)

```bash
curl -X POST http://localhost:8090/api/v1/loans \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "excellent-client-001",
    "amount": 30000,
    "termMonths": 36,
    "purpose": "Home improvement"
  }'
```

**Expected:** Auto-approved if credit score >= 750

### Scenario 2: Auto-Reject (Low Credit)

```bash
curl -X POST http://localhost:8090/api/v1/loans \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "low-credit-client-002",
    "amount": 10000,
    "termMonths": 24,
    "purpose": "Debt consolidation"
  }'
```

**Expected:** Auto-rejected if credit score < 500

### Scenario 3: Manual Review Required

```bash
curl -X POST http://localhost:8090/api/v1/loans \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "medium-client-003",
    "amount": 40000,
    "termMonths": 48,
    "purpose": "Business expansion"
  }'
```

**Expected:** Status = `PENDING_REVIEW`

**Approve manually:**

```bash
# Use Temporal CLI or UI to send signal
temporal workflow signal \
  --workflow-id loan-abc-123 \
  --name approveManually \
  --input '"Approved by manager"'
```

## Architecture Tests (ArchUnit)

The project includes ArchUnit tests to enforce ADR145 patterns:

```java
@Test
void workflows_must_not_have_cdi_injection() {
    // Workflows must be deterministic - no CDI!
}

@Test
void resources_should_be_thin_triggers() {
    // REST endpoints should NOT call repositories directly
}

@Test
void features_should_be_isolated() {
    // VSA slices must not depend on each other
}
```

Run with:

```bash
./gradlew test --tests 'ArchitectureTest'
```

## Key ADR145 Patterns Demonstrated

### 1. Thin Triggers

**REST Endpoint (`SubmitLoanResource.java`):**

```java
@POST
public Uni<Response> submitLoan(@Valid SubmitLoanRequest request) {
    // ONLY: Validate, trace, start workflow
    Span.current().setAttribute("client.id", request.clientId());

    WorkflowClient.start(workflow::processLoanApplication, command);

    return Response.accepted(...);
}
```

**Kafka Consumer (`LoanEventConsumer.java`):**

```java
@Incoming("loan-requests")
public CompletionStage<Void> consume(LoanRequestedEvent event) {
    // ONLY: Trace and start workflow (idempotent)
    WorkflowClient.start(workflow::processLoanApplication, command);
}
```

### 2. Smart Workflows

**Business Logic in Workflow (`SubmitLoanWorkflowImpl.java`):**

```java
public LoanResult processLoanApplication(LoanCommand command) {
    // Step 1: Save (Activity = I/O)
    loanId = activities.saveLoanApplication(command);

    // Step 2: Credit check (Child Workflow)
    CreditCheckResult credit = creditWorkflow.checkCredit(command.clientId());

    // Step 3: BUSINESS LOGIC (pure, deterministic)
    decision = evaluateLoanApplication(command, credit);

    // Step 4: Handle decision
    if (decision.requiresManualReview()) {
        Workflow.await(() -> "APPROVED".equals(status));
    }

    // Step 5: Update decision (Activity = I/O)
    activities.updateLoanDecision(loanId, status, reason);

    // Step 6: Publish event (Activity = I/O)
    activities.publishLoanDecisionEvent(loanId, clientId, decision);
}
```

### 3. Dumb Activities

**I/O Wrapper (`SubmitLoanActivitiesImpl.java`):**

```java
@Override
public String saveLoanApplication(LoanCommand command) {
    // NO business logic - just I/O
    return repository.save(entity)
        .map(LoanApplicationEntity::getId)
        .await().atMost(TIMEOUT);
}
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `loans` | Database name |
| `DB_USER` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |
| `TEMPORAL_ADDRESS` | `localhost:7233` | Temporal server address |
| `TEMPORAL_NAMESPACE` | `default` | Temporal namespace |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4317` | OTEL collector endpoint |
| `ENV` | `local` | Environment (local/dev/prod) |

## Monitoring & Observability

### Health Check

```bash
curl http://localhost:8090/q/health
```

### Metrics (Prometheus)

```bash
curl http://localhost:8090/q/metrics
```

### Distributed Tracing (Jaeger)

All requests are traced with business IDs:
- `client.id`: Client identifier
- `loan.id`: Loan application ID

View traces at: `http://localhost:16686`

## Database Schema

**Loan Applications Table:**

```sql
CREATE TABLE loan_applications (
    id VARCHAR(50) PRIMARY KEY,
    client_id VARCHAR(50) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    term_months INTEGER NOT NULL,
    purpose VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'INITIATED',
    decision_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

## Troubleshooting

### Issue: Port 8080 Already in Use

**Solution:** Temporal UI uses port 8080. Change app port to 8090 in `application.yaml`:

```yaml
quarkus:
  http:
    port: 8090
```

### Issue: Temporal Connection Refused

**Solution:** Ensure Temporal is running:

```bash
docker-compose ps temporal
docker-compose logs temporal
```

### Issue: Database Connection Failed

**Solution:** Check PostgreSQL is running and credentials are correct:

```bash
docker-compose ps postgres
docker exec -it loan-postgres psql -U postgres -d loans
```

### Issue: Native Build Fails

**Solution:** Increase Docker memory to at least 8GB, or use JVM build instead:

```bash
./gradlew build  # JVM build
```

## Contributing

This is a reference implementation for ADR145 architecture. When adding new features:

1. Follow VSA: Create a new feature package under `features/`
2. Implement thin triggers (REST/Kafka)
3. Put business logic in workflows (deterministic, no I/O)
4. Create dumb activities (simple I/O wrappers)
5. Add ArchUnit tests to enforce patterns
6. Write unit tests for workflows using `TestWorkflowEnvironment`

## License

Copyright © 2024 Addi. All rights reserved.

## References

- [ADR145: GenAI-Native Factory Backend Architecture](docs/adr/ADR145.md)
- [Quarkus Documentation](https://quarkus.io/)
- [Temporal Documentation](https://docs.temporal.io/)
- [Quarkus Temporal Extension](https://github.com/quarkiverse/quarkus-temporal)
