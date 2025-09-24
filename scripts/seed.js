import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { parse } from 'csv-parse/sync';
import { getDatabase } from '../src/main/db.js';
import { logger } from '../src/main/logger.js';
import { MASTER_HEADER_ALIASES, LEDGER_HEADER_ALIASES } from '../src/main/alias-registry.js';
import { normalizeHeaderName } from '../src/main/utils/headerMap.js';
import { formatDate, toPacific } from '../src/main/utils/time.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

function createNormalizedMap(record) {
  const map = new Map();
  Object.entries(record).forEach(([key, value]) => {
    map.set(normalizeHeaderName(key), value);
  });
  return map;
}

function resolveField(map, canonical, aliasSet) {
  const aliases = aliasSet[canonical] || [canonical];
  for (const candidate of aliases) {
    const normalized = normalizeHeaderName(candidate);
    if (map.has(normalized)) {
      return map.get(normalized);
    }
  }
  return null;
}

function normalizePhone(phone) {
  if (!phone) {
    return null;
  }
  const digits = phone.toString().replace(/\D+/g, '');
  return digits || null;
}

function normalizeDate(value) {
  return formatDate(value) || value || null;
}

function normalizeDateTime(value) {
  const dt = toPacific(value);
  return dt ? dt.toISO({ suppressMilliseconds: true }) : value || null;
}

function seedMaster(db) {
  const masterPath = path.resolve(__dirname, '../fixtures/master.sample.csv');
  const content = fs.readFileSync(masterPath, 'utf8');
  const records = parse(content, { columns: true, skip_empty_lines: true });
  db.prepare('DELETE FROM master').run();
  const insert = db.prepare(`INSERT INTO master (
    RootApptID, VisitDate, Customer, Phone, PhoneNorm, Email, EmailLower,
    VisitType, VisitNumber, SO, Brand, SalesStage, ConversionStatus,
    CustomOrderStatus, CenterStoneOrderStatus, AssignedRep, AssistedRep
  ) VALUES (
    @RootApptID, @VisitDate, @Customer, @Phone, @PhoneNorm, @Email, @EmailLower,
    @VisitType, @VisitNumber, @SO, @Brand, @SalesStage, @ConversionStatus,
    @CustomOrderStatus, @CenterStoneOrderStatus, @AssignedRep, @AssistedRep
  )`);

  const transaction = db.transaction((rows) => {
    for (const record of rows) {
      const map = createNormalizedMap(record);
      const phone = resolveField(map, 'Phone', MASTER_HEADER_ALIASES);
      const email = resolveField(map, 'Email', MASTER_HEADER_ALIASES);
      insert.run({
        RootApptID: resolveField(map, 'RootApptID', MASTER_HEADER_ALIASES),
        VisitDate: normalizeDate(resolveField(map, 'VisitDate', MASTER_HEADER_ALIASES)),
        Customer: resolveField(map, 'Customer', MASTER_HEADER_ALIASES),
        Phone: phone,
        PhoneNorm: normalizePhone(resolveField(map, 'PhoneNorm', MASTER_HEADER_ALIASES) || phone),
        Email: email,
        EmailLower: (resolveField(map, 'EmailLower', MASTER_HEADER_ALIASES) || email || '').toLowerCase(),
        VisitType: resolveField(map, 'VisitType', MASTER_HEADER_ALIASES),
        VisitNumber: resolveField(map, 'VisitNumber', MASTER_HEADER_ALIASES),
        SO: resolveField(map, 'SO', MASTER_HEADER_ALIASES),
        Brand: resolveField(map, 'Brand', MASTER_HEADER_ALIASES),
        SalesStage: resolveField(map, 'SalesStage', MASTER_HEADER_ALIASES),
        ConversionStatus: resolveField(map, 'ConversionStatus', MASTER_HEADER_ALIASES),
        CustomOrderStatus: resolveField(map, 'CustomOrderStatus', MASTER_HEADER_ALIASES),
        CenterStoneOrderStatus: resolveField(map, 'CenterStoneOrderStatus', MASTER_HEADER_ALIASES),
        AssignedRep: resolveField(map, 'AssignedRep', MASTER_HEADER_ALIASES),
        AssistedRep: resolveField(map, 'AssistedRep', MASTER_HEADER_ALIASES),
      });
    }
  });
  transaction(records);
  logger.info('Seeded master table with %d rows', records.length);
}

function seedLedger(db) {
  const ledgerPath = path.resolve(__dirname, '../fixtures/ledger.sample.csv');
  const content = fs.readFileSync(ledgerPath, 'utf8');
  const records = parse(content, { columns: true, skip_empty_lines: true });
  db.prepare('DELETE FROM payments').run();
  const insert = db.prepare(`INSERT INTO payments (
    RootApptID, PaymentDateTime, DocType, AmountNet, DocStatus, Raw
  ) VALUES (
    @RootApptID, @PaymentDateTime, @DocType, @AmountNet, @DocStatus, @Raw
  )`);
  const transaction = db.transaction((rows) => {
    for (const record of rows) {
      const map = createNormalizedMap(record);
      insert.run({
        RootApptID: resolveField(map, 'RootApptID', LEDGER_HEADER_ALIASES),
        PaymentDateTime: normalizeDateTime(resolveField(map, 'PaymentDateTime', LEDGER_HEADER_ALIASES)),
        DocType: resolveField(map, 'DocType', LEDGER_HEADER_ALIASES),
        AmountNet: Number(resolveField(map, 'AmountNet', LEDGER_HEADER_ALIASES) || 0),
        DocStatus: resolveField(map, 'DocStatus', LEDGER_HEADER_ALIASES),
        Raw: JSON.stringify(record),
      });
    }
  });
  transaction(records);
  logger.info('Seeded payments table with %d rows', records.length);
}

export function seedDatabase() {
  const db = getDatabase();
  seedMaster(db);
  seedLedger(db);
}

if (process.argv[1] === __filename) {
  seedDatabase();
}
