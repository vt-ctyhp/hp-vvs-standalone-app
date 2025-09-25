const { contextBridge } = require('electron');

function resolveServiceBaseUrl() {
  const arg = process.argv.find((value) => value.startsWith('serviceBaseUrl='));
  if (arg) {
    return arg.split('=')[1];
  }
  return process.env.SERVICE_BASE_URL || 'http://localhost:8080';
}

function parseBoolean(value) {
  if (typeof value !== 'string') {
    return false;
  }
  const normalized = value.trim().toLowerCase();
  return normalized === 'true' || normalized === '1' || normalized === 'yes';
}

function resolveFlagFromArgsOrEnv(argKey, envKey) {
  const arg = process.argv.find((value) => value.startsWith(`${argKey}=`));
  if (arg) {
    return parseBoolean(arg.split('=')[1]);
  }
  const envValue = envKey ? process.env[envKey] : undefined;
  return parseBoolean(envValue);
}

function resolveFeatureFlags() {
  return {
    diamonds: resolveFlagFromArgsOrEnv('FEATURE_DIAMONDS', 'FEATURE_DIAMONDS'),
    payments: resolveFlagFromArgsOrEnv('paymentsEnabled', 'FEATURE_PAYMENTS'),
    reports: resolveFlagFromArgsOrEnv('FEATURE_REPORTS', 'FEATURE_REPORTS'),
  };
}

const featureFlags = resolveFeatureFlags();

contextBridge.exposeInMainWorld('appConfig', {
  serviceBaseUrl: resolveServiceBaseUrl(),
  featureFlags,
  paymentsEnabled: Boolean(featureFlags.payments),
  reportsEnabled: Boolean(featureFlags.reports),
});
