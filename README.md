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
