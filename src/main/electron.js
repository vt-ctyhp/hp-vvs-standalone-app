import { app, BrowserWindow, ipcMain } from 'electron';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { AppointmentSummary } from '../domain/AppointmentSummary.js';
import { getSchedulerService } from '../adapters/SchedulerService.js';
import { logger } from './logger.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

function createWindow() {
  const win = new BrowserWindow({
    width: 1024,
    height: 768,
    webPreferences: {
      preload: path.join(__dirname, '../ui/preload.js'),
    },
  });

  win.loadFile(path.join(__dirname, '../ui/index.html'));
}

function setupSchedulers() {
  const scheduler = getSchedulerService();
  scheduler.register('morningSnapshot', { hour: 8, minute: 30 }, () => {
    logger.info('Morning snapshot job (stub) executed.');
  });
  scheduler.register('middayQueues', { hour: 13, minute: 0 }, () => {
    logger.info('Midday queues job (stub) executed.');
  });
  scheduler.register('lateDayDashboard', { hour: 16, minute: 30 }, () => {
    logger.info('Late-day dashboard job (stub) executed.');
  });
}

ipcMain.handle('appointment-summary:run', async (event, filters = {}) => {
  const summary = new AppointmentSummary();
  return summary.run(filters);
});

app.whenReady().then(() => {
  createWindow();
  setupSchedulers();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    getSchedulerService().shutdown();
    app.quit();
  }
});
