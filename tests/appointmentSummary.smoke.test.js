import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import { before, after, test } from 'node:test';

const dataDir = path.resolve('./data');
const testDbPath = path.join(dataDir, 'test.db');
process.env.DATABASE_PATH = testDbPath;
process.env.PAYMENTS_400_FILE_ID = 'local-ledger';

let runMigrations;
let seedDatabase;
let AppointmentSummary;

before(async () => {
  if (fs.existsSync(testDbPath)) {
    fs.rmSync(testDbPath);
  }
  if (!fs.existsSync(dataDir)) {
    fs.mkdirSync(dataDir, { recursive: true });
  }
  ({ runMigrations } = await import('../scripts/migrate.js'));
  ({ seedDatabase } = await import('../scripts/seed.js'));
  ({ AppointmentSummary } = await import('../src/domain/AppointmentSummary.js'));
  runMigrations();
  seedDatabase();
});

after(() => {
  const artifacts = [testDbPath, `${testDbPath}-shm`, `${testDbPath}-wal`];
  artifacts.forEach((file) => {
    if (fs.existsSync(file)) {
      fs.rmSync(file);
    }
  });
});

test('appointment summary returns expected headers and rows', () => {
  const summary = new AppointmentSummary();
  const result = summary.run();
  assert.deepStrictEqual(result.headers, [
    'Visit Date',
    'RootApptID',
    'Customer',
    'Phone',
    'Email',
    'Visit Type',
    'Visit #',
    'SO#',
    'Brand',
    'Sales Stage',
    'Conversion Status',
    'Custom Order Status',
    'Center Stone Order Status',
    'Assigned Rep',
    'Assisted Rep',
  ]);
  assert.ok(result.rows.length >= 3);
  const firstRow = result.rows[0];
  assert.equal(firstRow[0], '2024-04-01');
  assert.equal(firstRow[1], 'HP-1001');
  assert.equal(firstRow[7], 'SO-9001');
  const vvsRow = result.rows.find((row) => row[1] === 'HP-1002');
  assert.ok(vvsRow);
  assert.equal(vvsRow[8], 'VVS');
  assert.equal(vvsRow[10], 'Won');
});
