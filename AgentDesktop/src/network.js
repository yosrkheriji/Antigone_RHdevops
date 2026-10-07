// Module de détection réseau - collecte les infos réseau (IP, SSID WiFi)
// L'agent ne fait AUCUNE vérification de SSID - c'est le backend qui gère toute la logique
const { exec } = require('child_process');
const os = require('os');

// Récupérer l'adresse IP locale
function getLocalIP() {
  const interfaces = os.networkInterfaces();
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        return iface.address;
      }
    }
  }
  return '127.0.0.1';
}

// Récupérer le SSID WiFi connecté (cross-platform)
function getWifiSSID() {
  return new Promise((resolve) => {
    const platform = process.platform;

    if (platform === 'win32') {
      // Windows : netsh
      exec('netsh wlan show interfaces', { encoding: 'utf-8' }, (error, stdout) => {
        if (error) { resolve(null); return; }
        const lines = stdout.split('\n');
        for (const line of lines) {
          const trimmed = line.trim();
          const match = trimmed.match(/^SSID\s+:\s+(.+)/);
          if (match && !trimmed.startsWith('BSSID')) {
            resolve(match[1].trim());
            return;
          }
        }
        resolve(null);
      });

    } else if (platform === 'darwin') {
      // macOS : essaie en0, puis en1
      const tryIface = (iface, fallback) => {
        exec(`networksetup -getairportnetwork ${iface}`, { encoding: 'utf-8' }, (err, stdout) => {
          if (!err && stdout) {
            const match = stdout.match(/Current Wi-Fi Network:\s*(.+)/);
            if (match) { resolve(match[1].trim()); return; }
          }
          if (fallback) fallback();
          else resolve(null);
        });
      };
      tryIface('en0', () => tryIface('en1', null));

    } else {
      // Linux (fallback)
      exec('iwgetid -r', { encoding: 'utf-8' }, (error, stdout) => {
        if (error) { resolve(null); return; }
        resolve(stdout.trim() || null);
      });
    }
  });
}

// Collecter les informations réseau (IP + SSID) - pas de vérification, juste collecte
async function getNetworkInfo() {
  const localIP = getLocalIP();
  const ssid = await getWifiSSID();
  
  return {
    localIP,
    ssid: ssid || ''
  };
}

module.exports = { getNetworkInfo, getLocalIP, getWifiSSID };
