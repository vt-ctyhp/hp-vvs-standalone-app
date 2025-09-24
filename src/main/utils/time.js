import { DateTime } from 'luxon';

const PACIFIC = 'America/Los_Angeles';

export function now() {
  return DateTime.now().setZone(PACIFIC);
}

export function toPacific(dateLike) {
  if (!dateLike) {
    return null;
  }
  if (DateTime.isDateTime(dateLike)) {
    return dateLike.setZone(PACIFIC, { keepLocalTime: false });
  }
  if (dateLike instanceof Date) {
    return DateTime.fromJSDate(dateLike, { zone: PACIFIC });
  }
  if (typeof dateLike === 'number') {
    return DateTime.fromMillis(dateLike, { zone: PACIFIC });
  }
  if (typeof dateLike === 'string') {
    const parsed = DateTime.fromISO(dateLike, { zone: PACIFIC });
    if (parsed.isValid) {
      return parsed;
    }
    const fallback = DateTime.fromFormat(dateLike, 'M/d/yyyy', { zone: PACIFIC });
    if (fallback.isValid) {
      return fallback;
    }
  }
  return null;
}

export function formatDate(dateLike) {
  const dt = toPacific(dateLike);
  return dt ? dt.toFormat('yyyy-LL-dd') : null;
}

export function formatDateTime(dateLike) {
  const dt = toPacific(dateLike);
  return dt ? dt.toFormat('yyyy-LL-dd HH:mm') : null;
}

export function parseDate(dateString) {
  const dt = toPacific(dateString);
  return dt ? dt.startOf('day') : null;
}

export function parseDateTime(dateString) {
  return toPacific(dateString);
}

export function isSameDay(dateA, dateB) {
  const a = toPacific(dateA);
  const b = toPacific(dateB);
  if (!a || !b) {
    return false;
  }
  return a.hasSame(b, 'day');
}

export function startOfDay(dateLike) {
  const dt = toPacific(dateLike);
  return dt ? dt.startOf('day') : null;
}

export function endOfDay(dateLike) {
  const dt = toPacific(dateLike);
  return dt ? dt.endOf('day') : null;
}

export default {
  now,
  toPacific,
  formatDate,
  formatDateTime,
  parseDate,
  parseDateTime,
  isSameDay,
  startOfDay,
  endOfDay,
};
