# HP VVS Sales Automation Desktop + Service

This repository hosts the Electron desktop shell and Spring Boot backend that power the HP/VVS sales automation flows. The backend exposes an HTTP API that the Electron UI calls to run flows like **Appointment Summary**.

## Repository Layout

- `docs/` – reference materials and the OpenAPI contract (`api.yaml`).
- `svc-java/` – Spring Boot service with Flyway migrations and adapters.
- `ui-electron/` – Electron shell that renders flows via HTTP calls.
- `infra/` – docker-compose for Postgres and environment configuration.
- `fixtures/` – CSV source data used for seeding the service database.
- `scripts/` – helper scripts (e.g. fixture seeding).

## Prerequisites

- Node.js 18+
- Java 17+
- Docker (for local Postgres via Compose)
- Gradle 8.x (if `gradle` is not available, install it before running service commands)

## Quickstart

1. **Start infrastructure**
   ```bash
   cd infra
   cp .env.sample .env
   docker compose up -d postgres
   ```

2. **Run database migrations**
   ```bash
   cd ../svc-java
   ./gradlew flywayMigrate
   ```

3. **Seed fixtures**
   ```bash
   cd ..
   ./scripts/seed.sh
   ```

4. **Start the backend service**
   ```bash
   cd svc-java
   ./gradlew bootRun
   ```

5. **Launch the Electron UI**
   ```bash
   cd ../ui-electron
   npm install
   SERVICE_BASE_URL=http://localhost:8080 npm run dev
   ```

## Testing

```bash
cd svc-java
./gradlew test
```

## API Contract

The HTTP endpoints are documented under `docs/api.yaml`. The Appointment Summary flow is available at `POST /appointment-summary/run` and returns the ordered columns specified by the context pack.

## Seeding Strategy

The fixture seeder uses the alias registry and header map utilities to load CSV files irrespective of column order. Headers are deliberately shuffled before loading to guarantee the canonical column resolution stays resilient.
