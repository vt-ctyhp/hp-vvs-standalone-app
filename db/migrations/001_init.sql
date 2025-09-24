BEGIN TRANSACTION;

CREATE TABLE IF NOT EXISTS master (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  RootApptID TEXT NOT NULL,
  VisitDate TEXT,
  Customer TEXT,
  Phone TEXT,
  PhoneNorm TEXT,
  Email TEXT,
  EmailLower TEXT,
  VisitType TEXT,
  VisitNumber TEXT,
  SO TEXT,
  Brand TEXT,
  SalesStage TEXT,
  ConversionStatus TEXT,
  CustomOrderStatus TEXT,
  CenterStoneOrderStatus TEXT,
  AssignedRep TEXT,
  AssistedRep TEXT
);

CREATE INDEX IF NOT EXISTS idx_master_root ON master(RootApptID);
CREATE INDEX IF NOT EXISTS idx_master_visit_date ON master(VisitDate);

CREATE TABLE IF NOT EXISTS client_status_log (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  LogDate TEXT,
  SalesStage TEXT,
  ConversionStatus TEXT,
  CustomOrderStatus TEXT,
  CenterStoneOrderStatus TEXT,
  NextSteps TEXT,
  DeadlineType TEXT,
  DeadlineDate TEXT,
  MoveCount INTEGER,
  AssistedRep TEXT,
  UpdatedBy TEXT,
  UpdatedAt TEXT
);

CREATE TABLE IF NOT EXISTS ack_snapshot (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  SnapshotDate TEXT,
  CapturedAt TEXT,
  RootApptID TEXT,
  Rep TEXT,
  Role TEXT,
  ScopeGroup TEXT,
  CustomerName TEXT,
  SalesStage TEXT,
  ConversionStatus TEXT,
  CustomOrderStatus TEXT,
  UpdatedBy TEXT,
  UpdatedAt TEXT,
  DaysSinceLastUpdate INTEGER,
  ClientStatusReportURL TEXT
);

CREATE TABLE IF NOT EXISTS ack_snapshot_log (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  SnapshotDate TEXT,
  CapturedAt TEXT,
  RootApptID TEXT,
  Rep TEXT,
  Role TEXT,
  ScopeGroup TEXT,
  CustomerName TEXT,
  SalesStage TEXT,
  ConversionStatus TEXT,
  CustomOrderStatus TEXT,
  UpdatedBy TEXT,
  UpdatedAt TEXT,
  DaysSinceLastUpdate INTEGER,
  ClientStatusReportURL TEXT
);

CREATE TABLE IF NOT EXISTS dashboard_weights (
  Stage TEXT PRIMARY KEY,
  Weight REAL
);

CREATE TABLE IF NOT EXISTS client_status_entries (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  RootApptID TEXT NOT NULL,
  LogDate TEXT,
  SalesStage TEXT,
  ConversionStatus TEXT,
  CustomOrderStatus TEXT,
  CenterStoneOrderStatus TEXT,
  NextSteps TEXT,
  DeadlineType TEXT,
  DeadlineDate TEXT,
  MoveCount INTEGER,
  AssistedRep TEXT,
  UpdatedBy TEXT,
  UpdatedAt TEXT
);

CREATE INDEX IF NOT EXISTS idx_client_status_entries_root ON client_status_entries(RootApptID);

CREATE TABLE IF NOT EXISTS payments (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  RootApptID TEXT,
  PaymentDateTime TEXT,
  DocType TEXT,
  AmountNet REAL,
  DocStatus TEXT,
  Raw TEXT
);

CREATE INDEX IF NOT EXISTS idx_payments_root ON payments(RootApptID);
CREATE INDEX IF NOT EXISTS idx_payments_date ON payments(PaymentDateTime);

CREATE TABLE IF NOT EXISTS meta (
  key TEXT PRIMARY KEY,
  value TEXT
);

COMMIT;
