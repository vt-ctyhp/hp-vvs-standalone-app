import { getDatabase } from '../main/db.js';
import { logger } from '../main/logger.js';
import { PER_CLIENT_LOG_HEADERS } from '../main/alias-registry.js';

const META_KEY_SCHEMA = 'perClientReport.headers';

class PerClientReportService {
  constructor(db) {
    this.db = db;
  }

  ensureSchema(headers) {
    const existing = this.getStoredSchema();
    if (!existing) {
      this.storeSchema(headers);
      return headers;
    }
    const normalizedExisting = JSON.stringify(existing);
    const normalizedIncoming = JSON.stringify(headers);
    if (normalizedExisting !== normalizedIncoming) {
      logger.warn('Per-client report headers mismatch. Keeping stored schema.');
    }
    return existing;
  }

  storeSchema(headers) {
    const payload = JSON.stringify(headers);
    this.db
      .prepare(`INSERT INTO meta (key, value) VALUES (@key, @value)
        ON CONFLICT(key) DO UPDATE SET value=excluded.value`)
      .run({ key: META_KEY_SCHEMA, value: payload });
  }

  getStoredSchema() {
    const row = this.db.prepare('SELECT value FROM meta WHERE key = @key').get({ key: META_KEY_SCHEMA });
    if (!row) {
      return null;
    }
    try {
      return JSON.parse(row.value);
    } catch (error) {
      logger.error('Failed to parse stored per-client schema: %o', error);
      return null;
    }
  }

  appendEntries(rootApptId, entries = []) {
    if (!entries.length) {
      return 0;
    }
    const headers = Object.keys(PER_CLIENT_LOG_HEADERS);
    this.ensureSchema(headers);
    const insert = this.db.prepare(`INSERT INTO client_status_entries (
      RootApptID, LogDate, SalesStage, ConversionStatus, CustomOrderStatus,
      CenterStoneOrderStatus, NextSteps, DeadlineType, DeadlineDate,
      MoveCount, AssistedRep, UpdatedBy, UpdatedAt
    ) VALUES (
      @RootApptID, @LogDate, @SalesStage, @ConversionStatus, @CustomOrderStatus,
      @CenterStoneOrderStatus, @NextSteps, @DeadlineType, @DeadlineDate,
      @MoveCount, @AssistedRep, @UpdatedBy, @UpdatedAt
    )`);
    const transaction = this.db.transaction((payload) => {
      for (const row of payload) {
        insert.run({ ...row, RootApptID: rootApptId });
      }
    });
    transaction(entries);
    return entries.length;
  }

  listEntries(rootApptId) {
    const stmt = this.db.prepare(`SELECT * FROM client_status_entries WHERE RootApptID = @rootApptId ORDER BY LogDate ASC, rowid ASC`);
    return stmt.all({ rootApptId });
  }
}

let serviceInstance;

export function getPerClientReportService() {
  if (!serviceInstance) {
    serviceInstance = new PerClientReportService(getDatabase());
  }
  return serviceInstance;
}

export default PerClientReportService;
