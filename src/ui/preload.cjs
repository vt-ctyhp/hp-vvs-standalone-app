const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('appAPI', {
  runAppointmentSummary: (filters) => ipcRenderer.invoke('appointment-summary:run', filters),
});
