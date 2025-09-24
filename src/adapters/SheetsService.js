import { getDatabase } from '../main/db.js';
import { formatDate } from '../main/utils/time.js';
import { logger } from '../main/logger.js';

const DEFAULT_STAGE_WEIGHTS = [
  { Stage: 'LEAD', Weight: 0.1 },
  { Stage: 'HOT LEAD', Weight: 0.2 },
  { Stage: 'CONSULT', Weight: 0.3 },
  { Stage: 'DIAMOND VIEWING', Weight: 0.5 },
  { Stage: 'DEPOSIT', Weight: 0.9 },
  { Stage: 'ORDER COMPLETED', Weight: 0.0 },
];

class SheetsService {
  constructor(db) {
    this.db = db;
  }

  getMasterRows(filters = {}) {
    const clauses = [];
    const params = {};

    if (filters.brand) {
      clauses.push('LOWER(Brand) = LOWER(@brand)');
      params.brand = filters.brand;
    }
    if (filters.rep) {
      clauses.push('LOWER(AssignedRep) = LOWER(@rep) OR LOWER(AssistedRep) = LOWER(@rep)');
      params.rep = filters.rep;
    }
    if (filters.startDate) {
      clauses.push('VisitDate >= @startDate');
      params.startDate = formatDate(filters.startDate);
    }
    if (filters.endDate) {
      clauses.push('VisitDate <= @endDate');
      params.endDate = formatDate(filters.endDate);
    }

    let sql = `SELECT RootApptID, VisitDate, Customer, Phone, PhoneNorm, Email, EmailLower,
      VisitType, VisitNumber, SO, Brand, SalesStage, ConversionStatus, CustomOrderStatus,
      CenterStoneOrderStatus, AssignedRep, AssistedRep
      FROM master`;
    if (clauses.length) {
      sql += ` WHERE ${clauses.join(' AND ')}`;
    }
    sql += ' ORDER BY VisitDate ASC, RootApptID ASC';

    const stmt = this.db.prepare(sql);
    return stmt.all(params).map((row) => ({
      VisitDate: row.VisitDate,
      RootApptID: row.RootApptID,
      Customer: row.Customer,
      Phone: row.Phone || row.PhoneNorm,
      Email: row.Email || row.EmailLower,
      VisitType: row.VisitType,
      VisitNumber: row.VisitNumber,
      SO: row.SO,
      Brand: row.Brand,
      SalesStage: row.SalesStage,
      ConversionStatus: row.ConversionStatus,
      CustomOrderStatus: row.CustomOrderStatus,
      CenterStoneOrderStatus: row.CenterStoneOrderStatus,
      AssignedRep: row.AssignedRep,
      AssistedRep: row.AssistedRep,
    }));
  }

  getClientStatusLog(limit = 100) {
    const stmt = this.db.prepare(`SELECT * FROM client_status_log ORDER BY UpdatedAt DESC LIMIT @limit`);
    return stmt.all({ limit });
  }

  appendClientStatusLog(entries = []) {
    if (!entries.length) {
      return 0;
    }
    const insert = this.db.prepare(`INSERT INTO client_status_log (
      LogDate, SalesStage, ConversionStatus, CustomOrderStatus, CenterStoneOrderStatus,
      NextSteps, DeadlineType, DeadlineDate, MoveCount, AssistedRep, UpdatedBy, UpdatedAt
    ) VALUES (
      @LogDate, @SalesStage, @ConversionStatus, @CustomOrderStatus, @CenterStoneOrderStatus,
      @NextSteps, @DeadlineType, @DeadlineDate, @MoveCount, @AssistedRep, @UpdatedBy, @UpdatedAt
    )`);
    const transaction = this.db.transaction((rows) => {
      for (const row of rows) {
        insert.run(row);
      }
    });
    transaction(entries);
    return entries.length;
  }

  upsertAckSnapshot(rows = []) {
    const insert = this.db.prepare(`INSERT INTO ack_snapshot (
      SnapshotDate, CapturedAt, RootApptID, Rep, Role, ScopeGroup, CustomerName,
      SalesStage, ConversionStatus, CustomOrderStatus, UpdatedBy, UpdatedAt,
      DaysSinceLastUpdate, ClientStatusReportURL
    ) VALUES (
      @SnapshotDate, @CapturedAt, @RootApptID, @Rep, @Role, @ScopeGroup, @CustomerName,
      @SalesStage, @ConversionStatus, @CustomOrderStatus, @UpdatedBy, @UpdatedAt,
      @DaysSinceLastUpdate, @ClientStatusReportURL
    )`);
    const clear = this.db.prepare('DELETE FROM ack_snapshot');
    const transaction = this.db.transaction((data) => {
      clear.run();
      for (const row of data) {
        insert.run(row);
      }
    });
    transaction(rows);
  }

  appendAckSnapshotLog(rows = []) {
    if (!rows.length) {
      return;
    }
    const insert = this.db.prepare(`INSERT INTO ack_snapshot_log (
      SnapshotDate, CapturedAt, RootApptID, Rep, Role, ScopeGroup, CustomerName,
      SalesStage, ConversionStatus, CustomOrderStatus, UpdatedBy, UpdatedAt,
      DaysSinceLastUpdate, ClientStatusReportURL
    ) VALUES (
      @SnapshotDate, @CapturedAt, @RootApptID, @Rep, @Role, @ScopeGroup, @CustomerName,
      @SalesStage, @ConversionStatus, @CustomOrderStatus, @UpdatedBy, @UpdatedAt,
      @DaysSinceLastUpdate, @ClientStatusReportURL
    )`);
    const transaction = this.db.transaction((data) => {
      for (const row of data) {
        insert.run(row);
      }
    });
    transaction(rows);
  }

  getDashboardStageWeights() {
    const stmt = this.db.prepare('SELECT Stage, Weight FROM dashboard_weights ORDER BY Stage ASC');
    const rows = stmt.all();
    if (!rows.length) {
      return DEFAULT_STAGE_WEIGHTS;
    }
    return rows;
  }

  setDashboardStageWeights(weights = []) {
    const cleared = this.db.prepare('DELETE FROM dashboard_weights');
    const insert = this.db.prepare('INSERT INTO dashboard_weights (Stage, Weight) VALUES (@Stage, @Weight)');
    const transaction = this.db.transaction((data) => {
      cleared.run();
      for (const row of data) {
        insert.run(row);
      }
    });
    const payload = weights.length ? weights : DEFAULT_STAGE_WEIGHTS;
    transaction(payload);
    logger.info('Dashboard weights updated (%d rows)', payload.length);
  }
}

let sheetsServiceInstance;

export function getSheetsService() {
  if (!sheetsServiceInstance) {
    sheetsServiceInstance = new SheetsService(getDatabase());
  }
  return sheetsServiceInstance;
}

export default SheetsService;

