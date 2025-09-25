export function getServiceBaseUrl() {
  if (globalThis.appConfig && globalThis.appConfig.serviceBaseUrl) {
    return globalThis.appConfig.serviceBaseUrl;
  }
  return 'http://localhost:8080';
}
