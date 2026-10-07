// Module de popup de présence - affiche la fenêtre de confirmation périodique
const { BrowserWindow, ipcMain } = require('electron');
const path = require('path');
const config = require('./config');
const api = require('./api');

let popupWindow = null;
let popupInterval = null;

// ── Popup fin de journée ────────────────────────────────────────────────────
let clockOutPopupWindow = null;
let clockOutTimeout = null;
let clockOutScheduledDate = null; // Date (string) pour laquelle on a planifié

/**
 * Planifie la popup de fin de journée.
 * Apparaît à un moment aléatoire dans les 15 dernières minutes du shift.
 * Idempotent : ne replanifie pas si déjà fait pour aujourd'hui.
 */
function scheduleClockOutValidation() {
  const today = new Date().toDateString();
  if (clockOutScheduledDate === today) return; // Déjà planifié

  const heureFin = config.get('heureFinTravail') || '18:00';
  const [finH, finM] = heureFin.split(':').map(Number);

  const now = new Date();
  const finToday = new Date(now);
  finToday.setHours(finH, finM, 0, 0);

  // Offset aléatoire entre 1 et 14 minutes avant la fin du shift
  const randomOffsetMin = Math.floor(Math.random() * 14) + 1;
  const triggerTime = new Date(finToday.getTime() - randomOffsetMin * 60 * 1000);
  const msUntilTrigger = triggerTime.getTime() - now.getTime();

  if (msUntilTrigger <= 0) {
    console.log('[ClockOut] Heure de déclenchement déjà passée pour aujourd\'hui');
    return;
  }

  clockOutScheduledDate = today;
  if (clockOutTimeout) clearTimeout(clockOutTimeout);

  const minRestants = Math.floor(msUntilTrigger / 60000);
  console.log(`[ClockOut] Popup planifiée dans ~${minRestants} min (${randomOffsetMin} min avant fin de shift à ${heureFin})`);

  clockOutTimeout = setTimeout(() => {
    if (config.isLoggedIn() && config.get('clockedIn')) {
      showClockOutPopup(randomOffsetMin, finH, finM);
    } else {
      console.log('[ClockOut] Non connecté ou non clocké – popup annulée');
    }
  }, msUntilTrigger);
}

/**
 * Affiche la popup obligatoire de fin de journée.
 * L'employé doit cliquer "Valider" – la fenêtre ne peut pas être fermée autrement.
 */
function showClockOutPopup(minutesBeforeEnd, finH, finM) {
  if (clockOutPopupWindow && !clockOutPopupWindow.isDestroyed()) {
    clockOutPopupWindow.focus();
    return;
  }

  clockOutPopupWindow = new BrowserWindow({
    width: 460,
    height: 400,
    resizable: false,
    movable: true,
    minimizable: false,
    maximizable: false,
    closable: false,
    alwaysOnTop: true,
    skipTaskbar: false,
    frame: false,
    transparent: false,
    show: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true
    }
  });

  clockOutPopupWindow.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  clockOutPopupWindow.webContents.on('will-navigate', (event) => event.preventDefault());
  clockOutPopupWindow.loadFile(path.join(__dirname, 'popup.html'));

  clockOutPopupWindow.once('ready-to-show', () => {
    clockOutPopupWindow.show();
    clockOutPopupWindow.focus();

    const heureFinStr = `${String(finH).padStart(2, '0')}:${String(finM).padStart(2, '0')}`;
    clockOutPopupWindow.webContents.send('set-clockout-mode', {
      minutesBeforeEnd,
      heureFinTravail: heureFinStr
    });
  });

  clockOutPopupWindow.on('closed', () => {
    clockOutPopupWindow = null;
  });
}

function closeClockOutPopup() {
  if (clockOutPopupWindow && !clockOutPopupWindow.isDestroyed()) {
    clockOutPopupWindow.destroy();
    clockOutPopupWindow = null;
  }
}
// ────────────────────────────────────────────────────────────────────────────

// Démarrer le cycle de popups
function start() {
  const intervalHeures = config.get('popupIntervalleHeures') || 2;
  const intervalMs = intervalHeures * 60 * 60 * 1000;
  
  if (popupInterval) clearInterval(popupInterval);
  
  popupInterval = setInterval(() => {
    if (config.isLoggedIn() && config.isWorkingTime()) {
      showPopup();
    } else if (!config.isWorkingTime()) {
      console.log('[Popup] Hors horaires de travail - popup ignoré');
    }
  }, intervalMs);
  
  console.log(`[Popup] Service démarré - intervalle ${intervalHeures}h (actif uniquement pendant les horaires de travail)`);
}

// Arrêter le cycle
function stop() {
  if (popupInterval) {
    clearInterval(popupInterval);
    popupInterval = null;
  }
  closePopup();
  console.log('[Popup] Service arrêté');
}

// Afficher le popup de confirmation
function showPopup() {
  if (popupWindow) {
    popupWindow.focus();
    return;
  }
  
  popupWindow = new BrowserWindow({
    width: 420,
    height: 400,
    resizable: false,
    movable: true,
    minimizable: false,
    maximizable: false,
    closable: false,
    alwaysOnTop: true,
    skipTaskbar: false,
    frame: false,
    transparent: false,
    show: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true
    }
  });
  
  popupWindow.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  popupWindow.webContents.on('will-navigate', (event) => event.preventDefault());
  popupWindow.loadFile(path.join(__dirname, 'popup.html'));
  
  popupWindow.once('ready-to-show', () => {
    popupWindow.show();
    popupWindow.focus();
    
    // Envoyer le timeout configuré
    const timeout = config.get('popupTimeoutSecondes') || 60;
    popupWindow.webContents.send('set-timeout', timeout);
  });
  
  popupWindow.on('closed', () => {
    popupWindow = null;
  });
}

// Fermer le popup
function closePopup() {
  if (popupWindow && !popupWindow.isDestroyed()) {
    popupWindow.destroy();
    popupWindow = null;
  }
}

// Écouter la réponse du popup
function setupIPC() {
  ipcMain.on('presence-response', async (event, confirmed) => {
    console.log(`[Popup] Réponse reçue: ${confirmed ? 'CONFIRMÉ' : 'NON CONFIRMÉ'}`);
    
    // Envoyer au serveur
    await api.sendPresenceConfirm(confirmed);
    
    // Fermer le popup après un court délai
    setTimeout(() => {
      closePopup();
    }, confirmed ? 1000 : 500);
  });

  // ── IPC fin de journée ──────────────────────────────────────────────────
  ipcMain.on('clockout-confirmed', (event) => {
    console.log('[ClockOut] Validation reçue – fermeture de la popup');
    setTimeout(() => {
      closeClockOutPopup();
    }, 1200);
  });
}

module.exports = { start, stop, showPopup, setupIPC, scheduleClockOutValidation };
