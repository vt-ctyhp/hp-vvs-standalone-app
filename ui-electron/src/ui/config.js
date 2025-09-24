const DEFAULT_BASE_URL = 'http://localhost:8080';

export function getServiceBaseUrl() {
  const value = process.env.SERVICE_BASE_URL || DEFAULT_BASE_URL;
  return value.replace(/\/$/, '');
}
