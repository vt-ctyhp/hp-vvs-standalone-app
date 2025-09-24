import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { getDatabase } from '../src/main/db.js';
import { logger } from '../src/main/logger.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export function runMigrations() {
  const db = getDatabase();
  const migrationsDir = path.resolve(__dirname, '../db/migrations');
  const files = fs
    .readdirSync(migrationsDir)
    .filter((file) => file.endsWith('.sql'))
    .sort();

  for (const file of files) {
    const fullPath = path.join(migrationsDir, file);
    const sql = fs.readFileSync(fullPath, 'utf8');
    logger.info('Running migration %s', file);
    db.exec(sql);
  }
  logger.info('Migrations complete.');
}

if (process.argv[1] === __filename) {
  runMigrations();
}
