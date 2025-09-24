import { contextBridge, ipcRenderer } from 'electron';

contextBridge.exposeInMainWorld('appAPI', {
  runAppointmentSummary: (filters) => ipcRenderer.invoke('appointment-summary:run', filters),
});
