# HP / VVS Sales Automation — Desktop Local App

This repository hosts the Electron-based port of the HP/VVS Sales Automation system. It mirrors the workflows defined in the legacy Apps Script project while running entirely on a local Node.js + SQLite stack.

## Quickstart

1. **Install dependencies**
   ```bash
   npm install
   ```
2. **Configure environment**
   ```bash
   cp .env.sample .env
   # edit .env with IDs and local paths from the Context Pack
   ```
3. **Run database migrations**
   ```bash
   npm run migrate
   ```
4. **Seed local fixtures**
   ```bash
   npm run seed
   ```
5. **Launch the desktop app**
   ```bash
   npm run dev
   ```
6. **Run tests**
   ```bash
   npm test
   ```

## Architecture

The project follows a ports & adapters layout:

- `src/main/` — Electron main process utilities (configuration, logging, database handle).
- `src/adapters/` — Local implementations for workbook, ledger, Drive, scheduler, and per-client report services.
- `src/domain/` — Business flows. `AppointmentSummary` provides the initial read-only smoke test.
- `src/ui/` — Renderer assets for the Electron window.
- `db/migrations/` — SQLite schema migrations.
- `fixtures/` — CSV samples used to populate the local database.
- `tests/` — Node-based smoke tests.

All field names, aliases, and invariants trace back to [`docs/HP_VVS_Sales_Automation_Context_Pack.md`](docs/HP_VVS_Sales_Automation_Context_Pack.md), which remains the single source of truth.

## Notes

- The runtime enforces Pacific Time across scheduling and date parsing utilities.
- Header access is always alias-driven; the helper utilities tolerate order changes and heal missing columns.
- External integrations (OpenAI, Drive, Sheets) are stubbed locally for the v0 milestone.

## Phase 4 Payments

The payments module is guarded by the `FEATURE_PAYMENTS` flag. Backend tests flip the flag automatically; when running the service manually, export `FEATURE_PAYMENTS=true` to expose the endpoints and Electron panel.

End-to-end smoke verification:

```bash
docker compose up -d postgres
cd svc-java
./gradlew clean test        # Phase 4 integration tests run with the flag enabled
./gradlew bootRun           # (optional) start the service locally with FEATURE_PAYMENTS=true
cd ..
SERVICE_BASE_URL=http://localhost:8080 FEATURE_PAYMENTS=true bash scripts/verify_phase4.sh
```

The script posts an invoice and receipt, validates the summary math/filters, and replays the payload to confirm idempotency. On success it prints `Phase 4 CLI verification completed successfully.`.

## Phase 3 Diamonds

Diamonds order approvals, delivery, and stone decisions are hidden by default. Export `FEATURE_DIAMONDS=true` when you want to expose the Phase 3 flows:

- **Backend tests** — only the diamonds suites run with the flag enabled:
  ```bash
  cd svc-java
  FEATURE_DIAMONDS=true ./gradlew test --tests 'com.hpvvssalesautomation.diamonds.*'
  ```
- **CLI smoke** — seeds the 200_/100_ tables from `fixtures/diamonds.sample.csv`, exercises each endpoint twice, and asserts idempotency:
  ```bash
  docker compose -f infra/docker-compose.yml --profile service up -d
  FEATURE_DIAMONDS=true ./scripts/verify_phase3.sh
  docker compose -f infra/docker-compose.yml --profile service down
  ```
- **Electron UI** — launch with the flag to reveal the diamonds buttons (`FEATURE_DIAMONDS=true npm run dev`).

On success the smoke script prints `Phase 3 CLI verification completed successfully.`.

## Staging

Use the helper scripts to bring the stack up with both feature flags enabled for smoke testing:

- Enable staging:
  ```bash
  bash scripts/staging_enable.sh
  ```
  This exports `FEATURE_DIAMONDS=true` and `FEATURE_PAYMENTS=true`, launches Postgres and `svc-java`, seeds the database, and executes the Phase 3 and Phase 4 smoke suites.
- Disable staging:
  ```bash
  bash scripts/staging_disable.sh
  ```
  The script unsets the feature flags and stops the `svc-java` container, restoring the default state where both features remain hidden.
