import { format } from 'node:util';
import { DateTime } from 'luxon';

const LEVELS = ['debug', 'info', 'warn', 'error'];

function log(level, message, ...args) {
  if (!LEVELS.includes(level)) {
    level = 'info';
  }
  const timestamp = DateTime.now().setZone('America/Los_Angeles').toISO();
  const line = typeof message === 'string' ? format(message, ...args) : format('%o', message);
  const output = `[${timestamp}] [${level.toUpperCase()}] ${line}`;
  // eslint-disable-next-line no-console
  console[level === 'debug' ? 'log' : level](output);
}

export const logger = {
  debug: (msg, ...args) => log('debug', msg, ...args),
  info: (msg, ...args) => log('info', msg, ...args),
  warn: (msg, ...args) => log('warn', msg, ...args),
  error: (msg, ...args) => log('error', msg, ...args),
};

export default logger;
