# Context Pack — HP / VVS Sales Automation → Desktop Local (Electron)

**Goal:** Rebuild the existing Google Apps Script system as a local desktop app (Node/Electron + HTML UI). Keep behaviors and data contracts **identical** unless called out below.

---

## 0) High‑level system map

**Primary workbook (the “100_ file”)**  
- **Main tab:** `00_Master Appointments` — *single source of truth* for customer rows and visit history. Multiple modules read/write via robust header matching (aliases). 【turn27file3】  
- **Other in‑workbook tabs used by code:**  
  - `03_Client_Status_Log` (historical log per client row; also mirrored to per‑client reports), `07_Root_Index`, `08_Reps_Map`, `00_Dashboard`, `100_Metrics_View`. These names are referenced in the dashboard code and helper constants. 【turn26file6】

**Per‑client report files**  
- Each client has a dedicated Google Sheet (created on demand) with a tab **“Client Status”** whose **header row is 14**; columns listed under §3.2. The app appends log rows here and updates a summary “snapshot” block on the sheet. 【turn26file3】【turn26file8】

**Payments ledger workbook (“400_ ledger”)**  
- Tab: **`Payments`**. Code reads/writes using **tolerant header detection** for: `RootApptID`, `PaymentDateTime`, `DocType`, `AmountNet`, and (optional) `DocStatus`. These columns may appear under flexible aliases (e.g., *“Payment Date”, “Paid At”* for date; *“Net”, “Net Amount”* for amount). Ignore void/reversed rows. 【turn25file2】

**Acknowledgements (daily ops)**  
- Uses two snapshot tabs in the main workbook: **`13_Morning_Snapshot`** and **`14_Snapshot_Log`** with explicit headers (see §3.3). A morning trigger captures the expected set at ~8:30 AM PT and appends audit rows. Queues per rep are built from that snapshot. 【turn25file16】【turn26file2】

**Dashboard**  
- Works entirely off in‑memory reads + a “metrics” view (`100_Metrics_View`) and writes KPI blocks on `00_Dashboard`. Stage weights live at **`00_Dashboard!AS30:AT60`** (defaults provided if blank). Preset date filters write to **B1/B2/D2**; brand/rep filters live in **H1:I1 / H2:I2** (reads use the left/top cell). 【turn26file6】【turn26file14】

---

## 1) Runtime & configuration (to replicate)

**Default timezone & scopes (source project)**  
- Timezone is **America/Los_Angeles**; code often formats date‑only logs and schedules around PT. Preserve this as app default unless overridden. 【turn25file3】

**Script Properties (required keys)**  
Codex must surface a settings panel or `.env` for these (names **exactly** as listed; many are referenced by alias maps in code).  
- IDs & templates:  
  `SPREADSHEET_ID`, `PAYMENTS_400_FILE_ID`, `HPUSA_301_FILE_ID`, `VVS_302_FILE_ID`,  
  `HPUSA_SO_ROOT_FOLDER_ID`, `VVS_SO_ROOT_FOLDER_ID`,  
  `INTAKE_TEMPLATE_ID_HPUSA`, `INTAKE_TEMPLATE_ID_VVS`,  
  `CHECKLIST_TEMPLATE_ID_HPUSA`, `CHECKLIST_TEMPLATE_ID_VVS`,  
  `QUOTATION_TEMPLATE_ID_HPUSA`, `QUOTATION_TEMPLATE_ID_VVS`. 【turn25file10】  
- Per‑client report template token & analyzer:  
  `CS_REPORT_TEMPLATE_ID`, `REPORT_REANALYZE_TOKEN` (note: one variant key is misspelled `REPORT_REANLYZE_TOKEN` and exists in props; tolerate both). 【turn25file10】  
- OpenAI & webhooks:  
  `OPENAI_API_KEY`, `TEAM_CHAT_WEBHOOK`, `MANAGER_CHAT_WEBHOOK`, `WEBAPP_EXEC_URL`. 【turn25file10】  
- Misc flags / debug: `DEBUG`, `DEBUG_STRATEGIST`, `RP_DEBUG`, `REMIND_DEBUG_TEAM`, plus brand‑specific doc template IDs (e.g., `HPUSA_DR_TEMPLATE_ID`, `VVS_SR_TEMPLATE_ID`). 【turn25file10】

> **Finding IDs from URLs:** the legacy system extracts a Drive ID from **either** `/d/<id>` **or** `?id=<id>` or any 25+ char token. Preserve this tolerant behavior. 【turn25file3】

---

## 2) Triggers & automation (behavior to preserve)

**What currently runs on schedules (Apps Script → Desktop cron/daemon):**  
- **Acknowledgements suite:**  
  - **Morning**: build expected set & snapshot (~8:25–8:30 AM PT), build queues and dashboard.  
  - **Midday**: optional queues refresh (1:00 PM PT).  
  - **Late‑day**: optional dashboard rebuild (4:30 PM PT). Codex: replicate as local scheduled jobs. 【turn26file17】【turn25file16】  
- **Dashboard hourly** and **reminders daily** exist in the project; include equivalent schedulers. The project’s packer also snapshots triggers into `TRIGGERS.json`. 【turn25file1】

**Pack/export behavior (for your knowledge pack and parity checks):**  
- The project can **export a pack** with: `FUNCTION_INDEX.json`, `SCRIPT_PROPERTY_KEYS.json`, a triggers snapshot, and discovered sheet schemas for each configured workbook ID. Preserve a similar “export state” diagnostic in the desktop app for parity debugging. 【turn25file1】【turn27file2】

---

## 3) Data contracts (tabs, headers, shapes)

### 3.1 Master workbook: `00_Master Appointments`

This is the **authoritative** table of customers and appointments. Many modules use robust “first header index by alias” matchers (e.g., `Visit Date`, `RootApptID`, `Customer Name`, `Assigned Rep`/`Assisted Rep`, `SO#`, `Sales Stage`, `Conversion Status`, `Custom Order Status`, `Center Stone Order Status`, `Brand`, `Phone`, `Email`, `PhoneNorm`, `EmailLower`). A dedicated “Appointment Summary” feature confirms these columns and returns a canonical view:  

**Appointment Summary output columns (order):**  
`Visit Date, RootApptID, Customer, Phone, Email, Visit Type, Visit #, SO#, Brand, Sales Stage, Conversion Status, Custom Order Status, Center Stone Order Status` (plus `Assigned Rep`, `Assisted Rep` in the server config). Keep the aliases tolerant during reads. 【turn27file3】

### 3.2 Per‑client report (Google Sheet per customer)

- **Tab name:** `Client Status`.  
- **Header row:** **14**.  
- **Required columns** (exact names used for writes/appends):  
  `Log Date | Sales Stage | Conversion Status | Custom Order Status | Center Stone Order Status | Next Steps | Deadline Type | Deadline Date | Move Count | Assisted Rep | Updated By | Updated At`.  
- The “Record Deadline” dialog reads the active row in **Master**, increments *# moved* counters, and appends to this per‑client log using the URL in **“Client Status Report URL”** on the Master row. Codex must keep the **exact** column names and row index (14). 【turn26file8】【turn26file3】【turn26file14】

> The **Client Status** submit flow also **ensures** this per‑client report exists; if the URL/ID is missing or invalid, the system creates one from a template, writes a `_Config` block (including `RootApptID` and the report ID), and then logs/snapshots the update. Preserve this lifecycle. 【turn26file10】

### 3.3 Acknowledgement snapshot/log (in main workbook)

- **Tabs:** `13_Morning_Snapshot`, `14_Snapshot_Log`.  
- **Header set** (both sheets share it and the code heals headers to match):  
  `Snapshot Date, Captured At, RootApptID, Rep, Role, Scope Group, Customer Name, Sales Stage, Conversion Status, Custom Order Status, Updated By, Updated At, Days Since Last Update, Client Status Report URL`. 【turn26file2】

### 3.4 `03_Client_Status_Log` (main workbook)

- Historical log of Client Status changes made from the dialog. A one‑time audit utility **adds** a trailing column **“In Production Status”** if missing (header only). Codex: keep header‑aware appends and header‑healing behavior. 【turn26file7】

### 3.5 Dashboard & metrics

- **Stage Weights table** lives at **`00_Dashboard!AS30:AT60`**. If empty, seed with defaults:

  | Stage             | Weight |
  |-------------------|--------|
  | LEAD              | 0.10   |
  | HOT LEAD          | 0.20   |
  | CONSULT           | 0.30   |
  | DIAMOND VIEWING   | 0.50   |
  | DEPOSIT           | 0.90   |
  | ORDER COMPLETED   | 0.00   |

  Reads are case‑insensitive; code falls back to the above set if the block is blank. 【turn25file14】

- **Filter inputs** on `00_Dashboard`:  
  presets at **B1** (merged B1:D1), date range **B2 .. D2**, brand at **H1:I1** (read **I1**), rep at **H2:I2** (read **I2**). The “preset” selector fills the dates for This/Last week, This/Last month, QTD, YTD. **Replicate this UX.** 【turn26file14】【turn26file6】

- Dashboard derives KPIs such as **Deposits (first‑time/all)** by combining Master with the **400 ledger**, and a **Weighted Pipeline** using Stage Weights (above). “Receipt” rows are considered only if `DocType` matches /receipt/i, net > 0, and not voided/cancelled. **Keep these filters.** 【turn25file2】【turn25file7】

### 3.6 400_ Payments ledger

- **Reads:** tolerant header aliases. Required fields when scanning:  
  - Root: `RootApptID` (alias: *Root Appt ID, ROOT, Root_ID*).  
  - When: `PaymentDateTime` (alias: *Payment DateTime, Payment Date, Paid At*).  
  - Type: `DocType` (alias: *Document Type*).  
  - Net: `AmountNet` (alias: *Net, Net Amount*).  
  - Optional: `DocStatus` (alias: *Status*).  
  Filter to “Receipt” doc types, positive net, and drop void/reversed/cancelled. Used to find **first real deposit** (ignore tiny holds) and to compile “all deposits in window”. 【turn25file14】【turn25file2】【turn25file17】

- **Writes:** the “Record Payment” dialog writes a ledger row object that includes (at least):  
  `PAYMENT_ID, Brand, RootApptID, SO#, AnchorType, BasketID, DocType, PaymentDateTime, Method, Reference, Notes, AmountGross, FeePercent, FeeAmount, AmountNet, AllocatedToSO, LinesJSON, Subtotal, Order Total_SO, Paid-To-Date_SO, Balance_SO, Submitted By, Submitted Date/Time`. Headers are **ensured/created** and writing uses a header map (order‑agnostic). **Keep this contract.** 【turn26file16】

---

## 4) Major modules — responsibilities & contracts

### 4.1 Conversations & Summaries Suite

**Purpose:** Ingest consult audio/transcripts, build **Scribe** (facts) and **Strategist** (analysis) JSONs, save them under the client folder (`04_Summaries`), optionally read **newest transcript** from `03_Transcripts`, and write a normalized snapshot & SYS table entry. 【turn25file4】【turn27file17】

**Key behaviors to preserve:**
- **Newest artifact selection**: prefer `__summary_corrected_*.json` over base `__summary_*.json`; similar logic for strategist `__analysis_*.json`. **Newest by lastUpdated or created time**. 【turn25file4】【turn25file19】  
- **Transcript fallback**: pick newest `.txt` in `03_Transcripts/` and inject a Drive **view URL** for reference. 【turn27file11】  
- **Master‑owned identity**: After Scribe, inject `customer_name`, `phone`, `email` from Master (using tolerant header indexes: prefers display columns, falls back to normalized). 【turn27file17】  
- **Upload queue**: a WebApp `doPost` adds items; workers `processUploadQueue` and `processSummariesWorker` batch process and write into SYS tables. Preserve *idempotent* workers and chunking semantics. 【turn26file5】  
- **Schema discipline**: internal diagnostics check that **every object with properties has a full `.required` array**; keep this strictness when calling OpenAI’s JSON schema. 【turn27file9】

**Inputs:** RootApptID, newest transcript (optional), existing Scribe/Strategist JSONs.  
**Outputs:** Saved Scribe/Strategist JSON + URLs; updated SYS_Consults row; refreshed per‑client report (when applicable). 【turn25file19】

---

### 4.2 Client Status (Server + Dialog)

**Purpose:** Let reps update Sales Stage / Conversion / Order statuses, **append** to both the **main log** (*03_Client_Status_Log*) and the **per‑client report** (*Client Status tab, row 14 headers*), then **snapshot** the summary block in the per‑client sheet. If the per‑client report is missing/invalid, **create** it and write a `_Config`. Also mirror “Updated By/At” back to Master if those columns exist. 【turn26file10】

**Key behaviors to preserve:**
- **Header‑aware writes**: build a map from the current header row; *order cannot be assumed*. 【turn26file10】  
- **One‑time audit**: if `03_Client_Status_Log` lacks **“In Production Status”**, append that header as the **last** column (no shifting). 【turn26file7】  
- **Date normalization** for input controls (ISO `YYYY-MM-DD`) from mixed formats. 【turn26file0】  
- **Allowed lists** for dialog dropdowns are read from sheet ranges (Stage/Conv/COS/Center Stone); keep this externalized (source: status dropdowns & “Stage Weights”). *Note: the code references reading “Dropdowns” and weight tables to drive UI; replicate through configuration rather than hardcoding.*

**Per‑client log contract:** see §3.2.  
**Snapshot block fields:** includes brand, client, APPT_ID, AssignedRep, order date, sales stage/state fields, and last update identity and time. These are written into fixed labeled cells; order date is specifically placed when a “Order Date:” label exists (matches current behavior). 【turn26file10】

---

### 4.3 Deadlines (3D / Production)

**Purpose:** A modal lets a rep set or move **3D** and **Production** deadlines for the selected Master row; it increments “# moved” counters on Master and appends a **deadline log** row to the client’s **Client Status** tab (row 14 headers). **Required Master headers** must be present before logging. 【turn26file8】【turn26file3】

- **Master sheet required headers** (row 1; order agnostic):  
  `3D Deadline`, `# of Times 3D Deadline Moved`, `Production Deadline`, `# of Times Prod. Deadline Moved`, `Client Status Report URL`. Optional: `Assisted Rep`. 【turn26file8】  
- **Target log columns (per‑client)**: exact names in §3.2. 【turn26file8】

---

### 4.4 Acknowledgements Suite (Queues + Snapshot + Dashboard)

**Purpose:** Daily coverage workflow that (A) computes **expected** acknowledgment workload per rep (including assisted coverage pairing), (B) writes a **Morning Snapshot** + **Snapshot Log**, (C) builds per‑rep **Ack queues** with headers and validations, and (D) renders a **Clients by Stage** dashboard sectioned by canonical stages. 【turn25file16】【turn26file1】【turn25file13】

**Key behaviors to preserve:**
- **Coverage pairing:** a “partner coverage” map (e.g., *Maria & Paul*) is honored; if both off ⇒ “Assisted Coverage Gap”. Otherwise, duties may route to partner with `Assigned` vs `Assisted` roles derived from original assignment. 【turn25file16】  
- **Queue header and DV:** Each rep’s tab adds **`Ack Status`** (validated dropdown) and **`Ack Note`** input columns; **Updated At** gets datetime format; autosize columns; freeze header row. 【turn26file1】  
- **Snapshot header healing:** both `13_Morning_Snapshot` and `14_Snapshot_Log` are actively **healed** to the canonical header set (see §3.3) and width‑adjusted (insert/delete columns to match). 【turn26file2】  
- **Stage‑bucketed dashboard list:** “Clients by Stage” sorts rows by *(1) has past visit* then by *oldest→newest* using the relevant date (last past or next future). Deposit/Won rows gain **financial enrichment** (First Deposit Date/Amount, Order Total, Paid‑to‑Date, Outstanding), if available. Keep this four‑step pipeline: **collect → bucket → sort → flatten with section headers**. 【turn25file8】【turn25file13】

**Triggers to replicate:** morning snapshot (~8:30), optional midday queue refresh (1:00 PM), late‑day dashboard refresh (4:30 PM). 【turn26file17】

---

### 4.5 Dashboard (KPI & cohort charts)

**Purpose:** Build `100_Metrics_View`, render KPI cards on `00_Dashboard`, and maintain a hidden **history block** for sparklines (columns **AA..AP**, header at row **5**). Stage Weights from `AS30:AT60`. Filter inputs and preset logic set **B1/B2/D2** and **I1/I2**. **No 99_* staging tabs** are required; code reads ledger live for counts/sums. 【turn26file6】【turn26file14】【turn25file2】

- **Weekly totals table** persisted in hidden area (for charts): `Week | Consultations | Diamond Viewings | First Deposits (#) | First Deposit Sum ($)`; numeric formats as in legacy. Replicate this layout/formatting. 【turn26file9】

---

### 4.6 Payments — Record & Summary

**Record Payment (dialog + server)**  
- **Prefill** shows prior payments (last payment date), order totals/paid‑to‑date/balance, and lines from Master/Orders when available. Provides “Build line from 3D” (formats a setting spec from tracker log into a line). **Keep this UX affordance.** 【turn27file4】【turn26file11】  
- **Key config alias map** (must be preserved): *many property names are accepted for each setting*. Example: `LEDGER_FILE_ID` may come from `PAYMENTS_400_FILE_ID`, `LEDGER_FILE_ID`, `PAYMENTS_LEDGER_FILE_ID`, etc. Codex: implement a **key alias resolver** using the lists in the legacy code. The map also covers **orders files/tabs**, **doc templates** (DI/DR/SI/SR per brand), **AR root folders**, **fees**, **SO_ROOT_FOLDER_ID**, and **SO_RECEIPT_MASTER_AMOUNT**. 【turn25file12】  
- **Doc role inference**: If the dialog leaves role blank, infer from DocType: *CREDIT, PROGRESS, DEPOSIT (for deposit invoice), FINAL (invoice), else SALES_RECEIPT if there are lines, otherwise PAYMENT_RECEIPT*. **Uppercase canonicalization** applies. 【turn26file16】  
- **Ledger write:** see the **row object** in §3.6; headers are ensured/created and the write is order‑agnostic via a header map. 【turn26file16】

**Payment Summary (read‑only)**  
- Server functions (`ps_*`) parse doc lines, guess doc types, fetch history for an anchor (APPT/SO), and export to PDF. Maintain these read/format behaviors. (Function set confirmed in index.) 【turn27file16】

---

### 4.7 Diamonds — propose, quotation, order, delivery, decisions

**Quotation / Settings update**  
- **Quotation Settings** expects a named anchor cell (e.g., *“Ring Settings”* anchor). Code scans a rectangular window from that anchor, **builds a header vector** (flexible matching), maps **columns by candidates**, and allows batched add/remove/save of rows with fields:  
  `product, styleDetail, metal, bandWidth, ringSize, freeUpgrade, onlineRetailerPrice, brilliantEarthPriceAfterTax, vvsPrice, yourSavings, link`. **At least one of Product/Style Detail is required per row**. Keep right‑aligned numeric fields listed. UI shows a small **Quotation chip** if a URL is available. 【turn25file5】【turn25file11】

**3D Tracker versions**  
- The update flow can read the 3D **Tracker “Log”** tab (or `3D Log` / `3D Revision Log`) to list versions with timestamp/revision and setting snippets (ring style/metal/band/size). The 3D **Tracker URL** comes from the `3D Tracker` cell on the Master row. **Robust file‑ID extraction** applies. 【turn26file18】

**Order Approvals / Confirm Delivery / Stone Decisions**  
- Present as dedicated dialogs with server helpers to read/write the `200_`/orders sheet and update the `100_` summary. Maintain the flows as separate steps with their existing function sets (see function index). **Do not change column names; rely on alias lookups**. 【turn27file6】【turn27file12】【turn27file13】

---

## 5) Read/Write rules & invariants (carry over exactly)

1) **Header‑agnostic I/O** everywhere  
   - Build **header maps** from the live sheet row and always write by **name** (with case‑insensitive lookup of first match). Do **not** hard‑address columns by index. Helpers like `createHeaderMapFromRow_`, `getHeaderMap_` are canonical. 【turn26file3】【turn27file14】

2) **Flexible alias matching**  
   - For common fields (RootApptID, SO#, PaymentDateTime, etc.), accept a **set of aliases** and pick the first present. This pattern is pervasive (e.g., ledger, dashboard, reports). **Codex must implement a central alias registry.** 【turn25file2】【turn26file12】

3) **Financial filters**  
   - When counting/summing deposits: include only **receipt‑type** docs, positive net, **exclude** void/reversed/cancelled; optionally restrict to brand/rep or date window. 【turn25file2】【turn25file17】

4) **Dates**  
   - Log dates as date‑only (midnight) for “Log Date”; include separate **“Updated At”** timestamps with datetime formatting. Keep PT defaults. 【turn26file3】【turn26file1】

5) **Morning Snapshot contract**  
   - Write/Heal headers exactly as in §3.3; log captures post‑coverage expansion (partner duties), and persists a daily audit to `14_Snapshot_Log`. 【turn26file2】【turn25file16】

6) **UI consistency**  
   - Dialogs resize to fit content; buttons disabled until valid; list/table rendering uses **batched DOM writes** and **delegated events** (performance patterns). Preserve these UX patterns when rebuilding. (Confirmed across dialogs like quotation & payments.) 【turn25file11】【turn26file11】

---

## 6) Reports (status/by‑rep/KPI) — shaping rules

- **By Rep / By Status** shapers **insert `Order Total` and `Total Pay To Date` immediately after `Visit Date`** and may optionally insert production columns (**`In Production Status`** + **`Production Deadline`**) after **`Custom Order Status`** when requested (`includeProductionCols=true`). **Assisted Rep** shows *“Assisted (LastUpdatedByNameOrEmail)”* by mapping the last updated email to a display name (cached). **Replicate these transforms exactly.** 【turn26file12】【turn27file19】

- Generic “shape” for status uses a fixed minimal column set:  
  `APPT_ID, Customer Name, Assigned Rep, Brand, SO#, Sales Stage, Conversion Status, Custom Order Status, Center Stone Order Status, Next Steps, Client Status Report URL`. 【turn27file19】

---

## 7) Function surface (for parity testing)

Your original project enumerates callable server/UI functions. Use this to define **feature parity tests** and menu wiring in the desktop app:

- **Appointment Summary**: `as_openAppointmentSummary`, `as_runAppointmentSummary`, + helpers. Output columns listed in §3.1. 【turn27file0】  
- **Client Status**: `cs_openStatusDialog_`, `cs_submitFromDialog`, `ensureReportConfig_`, etc. 【turn27file15】  
- **Deadlines**: `showRecordDeadlineDialog`, `getRecordDeadlineInit`, `saveRecordDeadline`. 【turn27file5】  
- **Payments**: `openRecordPayment`, `rp_init`, `rp_submit`, `rp_listDocNumbersForAnchor`, plus `ps_init` / `ps_exportPdf` for summary. 【turn27file10】【turn27file16】  
- **3D / Diamonds**: `start3d_*`, `open3DRevision` + `rev3d_*`, `dp_openProposeDiamonds`, `uq_openUpdateQuotationDiamonds`, order/confirm/decision dialog pairs. 【turn27file10】【turn27file6】  
- **Reminders & DV queues**: `DV_*` and `DVQ_*` functions (team/manager messages, daily urgent, propose nudges). Keep queue ID and date keying semantics identical. 【turn27file5】

---

## 8) Desktop‑local rebuild — adapter plan (no code here; architecture only)

Codex, implement **adapters** to preserve behaviors:

1) **Storage**  
   - **Sheets adapter**: emulate header‑map I/O against local stores (SQLite or on‑disk CSV/Parquet). Maintain **order‑agnostic writes** by column name and **alias matching**.  
   - **Per‑client report**: represent each report as a local workbook (SQLite table + a JSON `_Config` row) with a **“Client Status”** table whose **schema matches §3.2** and whose **header row index = 14** is preserved logically (store as metadata; the UI should still render “row 14 header” to mimic parity).  
   - **Ledger adapter**: a table `Payments` with tolerant aliases for the five required fields and additional columns used by **rp_submit** (§3.6).  

2) **File system (Drive)**  
   - Emulate `RootAppt Folder` with a directory tree: per‑client folder contains `03_Transcripts`, `04_Summaries`, `05_ChatLogs`. Newest‑file selection and naming rules must match (regexes for `__summary__`, `__summary_corrected__`, `__analysis__`). 【turn25file4】【turn27file11】

3) **Schedulers**  
   - Cron/daemon tasks to replicate **morning snapshot**, **midday queues**, **late‑day dashboard**, **hourly dashboard**, **daily reminders**. PT default. Logging & idempotency required (skip if already ran for day). 【turn26file17】

4) **OpenAI**  
   - Maintain **JSON‑schema strictness checks** (required arrays on every object), and keep the **newest transcript** loading and **Master identity injection** behaviors. 【turn27file9】【turn27file17】

5) **UI**  
   - **Modal dialogs** (payments, deadlines, client status, diamonds flows): keep button‑disable logic, batched rendering, and toast/resize behaviors. (Examples: `fitDialog`, batched DOM writes, delegated events). 【turn26file11】【turn25file11】  
   - **Dashboard**: replicate filter cells (preset/date/brand/rep) and the history/kpi blocks; copy the *preset* semantics (“this week”, “last month”, etc.). 【turn26file14】

---

## 9) Non‑goals / constraints (for Codex)

- **Do not** change header names or their row indices where specified (e.g., **Client Status row 14**). 【turn26file8】  
- **Do not** convert tolerant ID parsing or column aliasing into strict single‑name lookups. Keep alias resolution exactly as described. 【turn25file3】【turn25file14】  
- **Do not** drop the financial filters (receipt‑type, positive net, non‑void). Dashboard and KPIs depend on them. 【turn25file2】

---

## 10) Acceptance checks (ready‑made parity tests)

- **Log append parity:** Given a Master row with a valid *Client Status Report URL*, creating a 3D/Production deadline writes a row with the **12 exact columns** in §3.2 to the per‑client `Client Status` tab. 【turn26file8】  
- **Morning snapshot parity:** Running the morning job creates/updates `13_Morning_Snapshot` with the header set in §3.3 and appends to `14_Snapshot_Log`. **Headers heal** if width mismatches. 【turn26file2】  
- **Dashboard stage weights:** With `AS30:AT36` blank, seeding produces the exact six rows in §3.5. Weighted pipeline uses these values. 【turn25file14】  
- **Ledger scanning:** Counting “all deposits in window” uses the alias set and exclusion rules; returns **0** on missing `PAYMENTS_400_FILE_ID` fail‑soft. 【turn25file17】  
- **Record Payment write:** Submitting writes the full row object listed in §3.6; headers are created if missing; **DocRole** auto‑inferred if not supplied. 【turn26file16】

---

## 11) Appendix — Handy reference (names & files)

- **Key tabs (main workbook):**  
  `00_Master Appointments`, `03_Client_Status_Log`, `07_Root_Index`, `08_Reps_Map`, `13_Morning_Snapshot`, `14_Snapshot_Log`, `00_Dashboard`, `100_Metrics_View`. 【turn26file6】【turn26file2】  
- **Per‑client report (each customer):** `Client Status` (row‑14 headers) + `_Config`. 【turn26file8】  
- **Ledger workbook:** `Payments` tab; alias matching for key columns. 【turn25file2】  
- **Function indices** for: Appointment Summary, Client Status, Deadlines, Payments (RP/PS), Start/Assign 3D, Revisions, Diamonds flows, Reminders/Queues — use `FUNCTION_INDEX.json` as the authoritative list of callable functions/features to re‑expose. 【turn27file0】【turn27file15】

---

### Notes on provenance

- Timezone & scopes: **America/Los_Angeles**; export pack & sheet schema snapshots exist in Core & Config. 【turn25file3】【turn25file1】  
- Properties (IDs & tokens) come from the serialized `SCRIPT_PROPERTY_KEYS.json`. **Treat misspellings as tolerated variants** when present (e.g., `REPORT_REANLYZE_TOKEN`). 【turn25file10】  
- Stage weights & dashboard contracts: `00_Dashboard` code paths. 【turn25file14】【turn26file6】  
- Acknowledgements: pairing logic, snapshot healing, queue building, and scheduler times. 【turn25file16】【turn26file1】【turn26file17】  
- Payments: alias map, role inference, ledger write shape, and prefill behaviors. 【turn25file12】【turn26file16】【turn27file4】  
- Reports: log append to per‑client sheet and “shape” functions for By‑Rep/Status. 【turn26file3】【turn26file12】

---

## 12) What Codex should build first (recommended order)

1) **Adapters**: Sheets (header map + alias registry), Ledger, Per‑client Report, Drive‑like folders.  
2) **Schedulers**: morning snapshot → queues → dashboard; hourly dashboard; daily reminders.  
3) **UI**: Client Status dialog → Deadlines → Payments → Appointment Summary → Diamonds dialogs.  
4) **Reports**: By Rep / By Status shaping + exports.  
5) **Conversations**: Scribe/Strategist ingestion, newest artifact resolution, identity injection.
