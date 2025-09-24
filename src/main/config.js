import fs from 'node:fs';
import path from 'node:path';
import dotenv from 'dotenv';

const CONFIG_KEY_ALIASES = {
  REPORT_REANLYZE_TOKEN: 'REPORT_REANALYZE_TOKEN',
};

function loadEnvFile() {
  const explicitPath = process.env.ENV_PATH || process.env.CONFIG_PATH;
  const defaultPath = path.resolve('.env');
  const targetPath = explicitPath ? path.resolve(explicitPath) : defaultPath;
  if (fs.existsSync(targetPath)) {
    dotenv.config({ path: targetPath });
  }
}

loadEnvFile();

function normalizeKey(key) {
  return key ? key.toString().trim().toUpperCase() : '';
}

function resolveKey(key) {
  const normalized = normalizeKey(key);
  const alias = CONFIG_KEY_ALIASES[normalized];
  if (alias) {
    const aliasValue = process.env[alias];
    if (aliasValue !== undefined) {
      return aliasValue;
    }
  }
  return process.env[normalized] ?? process.env[key] ?? null;
}

export function get(key, fallback = null) {
  const value = resolveKey(key);
  if (value === null || value === undefined || value === '') {
    if (fallback !== null) {
      return fallback;
    }
    return null;
  }
  return value;
}

export function getBoolean(key, fallback = false) {
  const value = resolveKey(key);
  if (value === null || value === undefined || value === '') {
    return fallback;
  }
  return ['1', 'true', 'yes', 'on'].includes(value.toString().toLowerCase());
}

export function getNumber(key, fallback = null) {
  const value = resolveKey(key);
  if (value === null || value === undefined || value === '') {
    return fallback;
  }
  const num = Number(value);
  return Number.isNaN(num) ? fallback : num;
}

export function asObject() {
  return new Proxy(
    {},
    {
      get: (_, prop) => get(prop),
      has: (_, prop) => resolveKey(prop) !== null,
    },
  );
}

const config = {
  get,
  getBoolean,
  getNumber,
  asObject,
};

export default config;
