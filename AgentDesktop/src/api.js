// Client API pour communiquer avec le backend Spring Boot
const axios = require('axios');
const config = require('./config');
const { EventEmitter } = require('events');

const authEvents = new EventEmitter();
let reloginInProgress = null;

// Tentative de reconnexion silencieuse avec les credentials sauvegardés
// Retourne { ok: true } si succès
// Retourne { ok: false, isNetworkError: true } si le serveur est injoignable
// Retourne { ok: false, isNetworkError: false } si les credentials sont refusés par le serveur
async function silentRelogin() {
  if (reloginInProgress) return reloginInProgress;
  reloginInProgress = (async () => {
    try {
      const creds = config.getCredentials();
      if (!creds) {
        console.warn('[API] Aucun credential sauvegardé, reconnexion automatique impossible');
        return { ok: false, isNetworkError: false };
      }
      console.log('[API] Token expiré — tentative de reconnexion silencieuse...');
      const serverUrl = config.get('serverUrl');
      const response = await axios.post(`${serverUrl}/api/auth/login`, {
        username: creds.username,
        password: creds.password
      }, { timeout: 10000 });
      const apiResponse = response.data;
      if (!apiResponse.success || !apiResponse.data?.token) {
        return { ok: false, isNetworkError: false };
      }
      const loginData = apiResponse.data;
      config.set('token', loginData.token);
      config.set('isLoggedIn', true);
      config.set('employeId', loginData.employeId);
      config.set('nom', loginData.nom);
      config.set('prenom', loginData.prenom);
      console.log('[API] Reconnexion silencieuse réussie');
      return { ok: true, isNetworkError: false };
    } catch (err) {
      // Distinguer erreur réseau (serveur down) vs rejet credentials (serveur répond mais refuse)
      const isNetworkError = !err.response || err.code === 'ECONNREFUSED' || err.code === 'ECONNRESET' || err.code === 'ETIMEDOUT' || err.code === 'ENOTFOUND';
      if (isNetworkError) {
        console.warn('[API] Reconnexion silencieuse impossible — serveur injoignable:', err.message);
      } else {
        console.error('[API] Reconnexion silencieuse refusée par le serveur:', err.message);
      }
      return { ok: false, isNetworkError };
    } finally {
      reloginInProgress = null;
    }
  })();
  return reloginInProgress;
}

function getClient() {
  const serverUrl = config.get('serverUrl');
  const token = config.get('token');
  
  const client = axios.create({
    baseURL: serverUrl,
    timeout: 10000,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    }
  });

  // Intercepteur : si le serveur répond 401/403, tenter un re-login silencieux
  client.interceptors.response.use(
    response => response,
    async (error) => {
      const status = error?.response?.status;
      if ((status === 401 || status === 403) && !error.config._retried) {
        error.config._retried = true;
        const result = await silentRelogin();
        if (result.ok) {
          error.config.headers['Authorization'] = `Bearer ${config.get('token')}`;
          return axios(error.config);
        }
        if (result.isNetworkError) {
          // Serveur temporairement injoignable — ne pas effacer la session.
          // Marquer comme géré pour que handleAuthFailure ne supprime pas la session.
          error._handled = true;
          console.warn('[API] Reconnexion différée — serveur injoignable, session conservée');
          return Promise.reject(error);
        }
        // Le serveur a explicitement refusé les credentials : effacer la session
        config.clearSession();
        authEvents.emit('session-expired');
        error._handled = true;
      }
      return Promise.reject(error);
    }
  );

  return client;
}

// Login employé
async function login(username, password) {
  try {
    const serverUrl = config.get('serverUrl');
    const response = await axios.post(`${serverUrl}/api/auth/login`, {
      username,
      password
    });
    
    // La réponse est: { success, message, data: { compteId, employeId, username, ... } }
    const apiResponse = response.data;
    
    if (!apiResponse.success) {
      return { success: false, message: apiResponse.message || 'Identifiants incorrects' };
    }
    
    const loginData = apiResponse.data;
    if (!loginData?.token) {
      return { success: false, message: 'Token de session manquant dans la réponse serveur' };
    }
    config.set('employeId', loginData.employeId);
    config.set('username', loginData.username);
    config.set('nom', loginData.nom);
    config.set('prenom', loginData.prenom);
    config.set('token', loginData.token);
    config.set('isLoggedIn', true);
    config.saveCredentials(loginData.username, password);
    
    // Charger la config du serveur
    await fetchConfig();
    
    return { success: true, data: loginData };
  } catch (error) {
    handleAuthFailure(error);
    const message = error.response?.data?.message || error.message || 'Erreur de connexion';
    return { success: false, message };
  }
}

// Récupérer la configuration du serveur
// Récupérer la configuration du serveur — appelé 1 fois par jour max
async function fetchConfig() {
  // Vérifier si on a déjà fetch aujourd'hui (pour éviter les appels inutiles)
  const today = new Date().toISOString().slice(0, 10); // 'YYYY-MM-DD'
  const lastFetch = config.get('lastConfigFetchDate');
  if (lastFetch === today) {
    console.log('[API] Config déjà à jour pour aujourd\'hui (' + today + ') — utilisation du cache');
    return config.get('lastConfigSnapshot') || null;
  }

  try {
    const client = getClient();
    const response = await client.get('/api/agent/config');
    
    // Le backend retourne ApiResponse: { success, message, data: { config... } }
    const apiResponse = response.data;
    const serverConfig = apiResponse.data || apiResponse; // Fallback si pas de wrapper
    
    console.log('[API] Config reçue du serveur:', JSON.stringify(serverConfig));
    
    if (serverConfig.popupIntervalleHeures) config.set('popupIntervalleHeures', serverConfig.popupIntervalleHeures);
    if (serverConfig.popupTimeoutSecondes) config.set('popupTimeoutSecondes', serverConfig.popupTimeoutSecondes);
    if (serverConfig.inactiviteToleranceMinutesJour) config.set('inactiviteToleranceMinutesJour', serverConfig.inactiviteToleranceMinutesJour);
    if (serverConfig.reseauEntrepriseIp) config.set('reseauEntrepriseIp', serverConfig.reseauEntrepriseIp);
    if (serverConfig.reseauEntrepriseSsid !== undefined) config.set('reseauEntrepriseSsid', serverConfig.reseauEntrepriseSsid);
    if (serverConfig.toleranceRetardMinutes) config.set('toleranceRetardMinutes', serverConfig.toleranceRetardMinutes);
    // Horaires et jours de travail
    if (serverConfig.heureDebutTravail) config.set('heureDebutTravail', serverConfig.heureDebutTravail);
    if (serverConfig.heureFinTravail) config.set('heureFinTravail', serverConfig.heureFinTravail);
    if (serverConfig.joursTravail) config.set('joursTravail', serverConfig.joursTravail);
    // Pauses déjeuner
    config.set('pauseDebutMidi', serverConfig.pauseDebutMidi || null);
    config.set('pauseFinMidi', serverConfig.pauseFinMidi || null);
    // Jours fériés (tableau de strings 'YYYY-MM-DD')
    if (Array.isArray(serverConfig.joursFeries)) {
      config.set('joursFeries', serverConfig.joursFeries);
    }
    
    // Marquer la date du fetch et garder un snapshot
    config.set('lastConfigFetchDate', today);
    config.set('lastConfigRefresh', new Date().toISOString());
    config.set('lastConfigSnapshot', serverConfig);

    console.log('[API] Config appliquée: SSID=' + config.get('reseauEntrepriseSsid') +
      ', Horaires=' + config.get('heureDebutTravail') + '-' + config.get('heureFinTravail') +
      ', Pause=' + config.get('pauseDebutMidi') + '-' + config.get('pauseFinMidi') +
      ', Jours=' + config.get('joursTravail') +
      ', JoursFériés=' + (config.get('joursFeries') || []).length);
    
    return serverConfig;
  } catch (error) {
    handleAuthFailure(error);
    console.error('[API] Erreur chargement config:', error.message);
    return null;
  }
}

// Retry silencieux: tente l'appel, puis retente après délai si le serveur est down
async function withRetry(fn, retries = 3, delayMs = 5000) {
  for (let attempt = 1; attempt <= retries; attempt++) {
    try {
      return await fn();
    } catch (error) {
      const isNetworkError = !error.response || error.code === 'ECONNREFUSED' || error.code === 'ECONNRESET' || error.code === 'ETIMEDOUT';
      if (isNetworkError && attempt < retries) {
        console.log(`[API] Serveur injoignable, tentative ${attempt}/${retries}. Retry dans ${delayMs / 1000}s...`);
        await new Promise(r => setTimeout(r, delayMs));
      } else {
        throw error;
      }
    }
  }
}

// Envoyer heartbeat (chaque minute)
async function sendHeartbeat(ipAddress, ssid, actif) {
  try {
    const employeId = config.get('employeId');
    
    await withRetry(() => {
      const client = getClient();
      return client.post('/api/agent/heartbeat', {
        employeId,
        ipAddress: ipAddress || '',
        ssid: ssid || '',
        actif
      });
    });
    
    config.set('lastHeartbeat', new Date().toISOString());
    return true;
  } catch (error) {
    handleAuthFailure(error);
    console.error('[API] Erreur heartbeat:', error.message);
    return false;
  }
}

// Envoyer un événement (CLOCK_IN ou CLOCK_OUT) avec IP et SSID
async function sendEvent(eventType, ipAddress, ssid) {
  try {
    await withRetry(() => {
      const client = getClient();
      return client.post('/api/agent/event', {
        eventType,
        ipAddress: ipAddress || '',
        ssid: ssid || ''
      });
    });
    
    console.log('[API] Événement', eventType, 'envoyé (SSID:', ssid || 'N/A', ', IP:', ipAddress || 'N/A', ')');
    return true;
  } catch (error) {
    handleAuthFailure(error);
    console.error('[API] Erreur événement', eventType + ':', error.message);
    return false;
  }
}

// Confirmer la présence (popup)
async function sendPresenceConfirm(confirmed) {
  try {
    await withRetry(() => {
      const client = getClient();
      return client.post('/api/agent/presence-confirm', {
        confirmed
      });
    });
    
    console.log(`[API] Confirmation présence: ${confirmed}`);
    return true;
  } catch (error) {
    handleAuthFailure(error);
    console.error('[API] Erreur confirmation présence:', error.message);
    return false;
  }
}

function handleAuthFailure(error) {
  if (error?._handled) return; // Déjà géré par l'intercepteur axios
  if (error?.response?.status === 401 || error?.response?.status === 403) {
    console.warn('[API] Session agent expirée ou refusée, nettoyage de la session locale');
    config.clearSession();
    authEvents.emit('session-expired');
  }
}

module.exports = {
  login,
  fetchConfig,
  sendHeartbeat,
  sendEvent,
  sendPresenceConfirm,
  authEvents
};
