import { getDatabase } from '../main/db.js';
import { logger } from '../main/logger.js';
import { now, toPacific } from '../main/utils/time.js';

const META_PREFIX = 'scheduler.lastRun.';

class SchedulerService {
  constructor(db) {
    this.db = db;
    this.jobs = new Map();
  }

  getLastRun(name) {
    const row = this.db.prepare('SELECT value FROM meta WHERE key = @key').get({ key: META_PREFIX + name });
    if (!row) {
      return null;
    }
    return toPacific(row.value);
  }

  recordRun(name, timestamp) {
    const value = timestamp.toISO();
    this.db
      .prepare(`INSERT INTO meta (key, value) VALUES (@key, @value)
        ON CONFLICT(key) DO UPDATE SET value = excluded.value`)
      .run({ key: META_PREFIX + name, value });
  }

  shouldRun(job, referenceTime) {
    const { spec, name } = job;
    const lastRun = this.getLastRun(name);
    if (spec.oncePerDay !== false && lastRun && lastRun.hasSame(referenceTime, 'day')) {
      return false;
    }
    if (spec.daysOfWeek && spec.daysOfWeek.length) {
      if (!spec.daysOfWeek.includes(referenceTime.weekday)) {
        return false;
      }
    }
    const scheduledTime = referenceTime.set({ hour: spec.hour, minute: spec.minute || 0, second: 0, millisecond: 0 });
    if (referenceTime < scheduledTime) {
      return false;
    }
    if (lastRun && referenceTime.diff(lastRun, 'minutes').minutes < 1) {
      return false;
    }
    return true;
  }

  evaluate(job) {
    const referenceTime = now();
    if (!this.shouldRun(job, referenceTime)) {
      return;
    }
    try {
      Promise.resolve(job.fn()).finally(() => {
        this.recordRun(job.name, referenceTime);
        logger.info('Scheduler ran job %s at %s', job.name, referenceTime.toISO());
      });
    } catch (error) {
      logger.error('Scheduler job %s failed: %o', job.name, error);
    }
  }

  register(name, cronLikeSpec, fn) {
    const job = { name, spec: cronLikeSpec, fn };
    this.jobs.set(name, job);
    this.evaluate(job);
    job.timer = setInterval(() => this.evaluate(job), 60 * 1000);
    return () => this.unregister(name);
  }

  unregister(name) {
    const job = this.jobs.get(name);
    if (!job) {
      return;
    }
    if (job.timer) {
      clearInterval(job.timer);
    }
    this.jobs.delete(name);
  }

  shutdown() {
    for (const name of this.jobs.keys()) {
      this.unregister(name);
    }
  }
}

let schedulerInstance;

export function getSchedulerService() {
  if (!schedulerInstance) {
    schedulerInstance = new SchedulerService(getDatabase());
  }
  return schedulerInstance;
}

export default SchedulerService;
