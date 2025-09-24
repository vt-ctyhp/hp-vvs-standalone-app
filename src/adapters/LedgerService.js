import { getDatabase } from '../main/db.js';
import config from '../main/config.js';
import { formatDateTime } from '../main/utils/time.js';

class LedgerService {
  constructor(db) {
    this.db = db;
    this.enabled = Boolean(config.get('PAYMENTS_400_FILE_ID'));
  }

  receiptFilterWhereClauses() {
    return `LOWER(DocType) LIKE '%receipt%' AND AmountNet > 0
      AND COALESCE(LOWER(DocStatus), '') NOT IN ('void', 'reversed', 'cancelled')`;
  }

  getReceiptDeposits(filters = {}) {
    if (!this.enabled) {
      return [];
    }
    const clauses = [this.receiptFilterWhereClauses()];
    const params = {};
    if (filters.rootApptId) {
      clauses.push('RootApptID = @rootApptId');
      params.rootApptId = filters.rootApptId;
    }
    if (filters.startDate) {
      clauses.push('PaymentDateTime >= @startDate');
      params.startDate = formatDateTime(filters.startDate);
    }
    if (filters.endDate) {
      clauses.push('PaymentDateTime <= @endDate');
      params.endDate = formatDateTime(filters.endDate);
    }
    let sql = `SELECT RootApptID, PaymentDateTime, DocType, AmountNet, DocStatus
      FROM payments`;
    if (clauses.length) {
      sql += ` WHERE ${clauses.join(' AND ')}`;
    }
    sql += ' ORDER BY PaymentDateTime ASC';
    return this.db.prepare(sql).all(params);
  }

  firstRealDeposit(rootApptId) {
    const deposits = this.getReceiptDeposits({ rootApptId });
    return deposits.length ? deposits[0] : null;
  }

  depositsInWindow(startDate, endDate) {
    return this.getReceiptDeposits({ startDate, endDate });
  }

  depositTotalInWindow(startDate, endDate) {
    const deposits = this.depositsInWindow(startDate, endDate);
    if (!deposits.length) {
      return 0;
    }
    return deposits.reduce((acc, row) => acc + Number(row.AmountNet || 0), 0);
  }
}

let ledgerInstance;

export function getLedgerService() {
  if (!ledgerInstance) {
    ledgerInstance = new LedgerService(getDatabase());
  }
  return ledgerInstance;
}

export default LedgerService;
