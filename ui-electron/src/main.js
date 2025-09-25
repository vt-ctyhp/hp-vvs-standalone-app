import { app, BrowserWindow } from 'electron';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const serviceBaseUrl = process.env.SERVICE_BASE_URL ?? 'http://localhost:8080';
const paymentsEnabled = (process.env.FEATURE_PAYMENTS ?? 'false').toLowerCase() === 'true';
const diamondsEnabled = (process.env.FEATURE_DIAMONDS ?? 'false').toLowerCase() === 'true';
const reportsEnabled = (process.env.FEATURE_REPORTS ?? 'false').toLowerCase() === 'true';

function createWindow() {
  const win = new BrowserWindow({
    width: 1024,
    height: 768,
    webPreferences: {
      preload: path.join(__dirname, 'preload.cjs'),
      contextIsolation: true,
      nodeIntegration: false,
      additionalArguments: [
        `serviceBaseUrl=${serviceBaseUrl}`,
        `FEATURE_DIAMONDS=${diamondsEnabled}`,
        `paymentsEnabled=${paymentsEnabled}`,
        `FEATURE_REPORTS=${reportsEnabled}`,
      ],
    },
  });

  win.loadFile(path.join(__dirname, 'index.html'));
}

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
