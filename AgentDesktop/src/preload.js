const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('agentDesktop', {
  getRuntimeConfig: () => ipcRenderer.invoke('agent:get-runtime-config'),
  loginAttempt: (payload) => ipcRenderer.send('login-attempt', payload),
  onLoginResult: (callback) => {
    const handler = (_event, result) => callback(result);
    ipcRenderer.on('login-result', handler);
    return () => ipcRenderer.removeListener('login-result', handler);
  },
  minimizeLogin: () => ipcRenderer.send('login-minimize'),
  closeLogin: () => ipcRenderer.send('login-close'),
  onSetTimeout: (callback) => {
    const handler = (_event, seconds) => callback(seconds);
    ipcRenderer.on('set-timeout', handler);
    return () => ipcRenderer.removeListener('set-timeout', handler);
  },
  sendPresenceResponse: (confirmed) => ipcRenderer.send('presence-response', confirmed),
  // ── Popup fin de journée ─────────────────────────────────────────────────
  onSetClockoutMode: (callback) => {
    const handler = (_event, data) => callback(data);
    ipcRenderer.on('set-clockout-mode', handler);
    return () => ipcRenderer.removeListener('set-clockout-mode', handler);
  },
  sendClockoutConfirm: () => ipcRenderer.send('clockout-confirmed'),
});