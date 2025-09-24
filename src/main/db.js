import fs from 'node:fs';
import path from 'node:path';
import Database from 'better-sqlite3';
import config from './config.js';
import { logger } from './logger.js';

let dbInstance = null;

export function getDatabase() {
  if (dbInstance) {
    return dbInstance;
  }
  const configuredPath = config.get('DATABASE_PATH', './data/app.db');
  const resolvedPath = path.resolve(configuredPath);
  fs.mkdirSync(path.dirname(resolvedPath), { recursive: true });
  logger.debug('Opening SQLite database at %s', resolvedPath);
  dbInstance = new Database(resolvedPath);
  dbInstance.pragma('journal_mode = WAL');
  return dbInstance;
}

export default getDatabase;
