const fs = require('fs');
const path = require('path');
const { app } = require('electron');

const MAX_LOG_SIZE_BYTES = 5 * 1024 * 1024;
let initialized = false;
let logFilePath = null;

function initialize() {
  if (initialized) {
    return logFilePath;
  }

  app.setAppLogsPath();
  const logsDir = app.getPath('logs');
  fs.mkdirSync(logsDir, { recursive: true });
  logFilePath = path.join(logsDir, 'agent.log');

  patchConsole();
  initialized = true;
  console.info('[Logger] Logs persistants activés:', logFilePath);
  return logFilePath;
}

function patchConsole() {
  for (const level of ['log', 'info', 'warn', 'error']) {
    const original = console[level].bind(console);
    console[level] = (...args) => {
      original(...args);
      write(level.toUpperCase(), args);
    };
  }
}

function write(level, args) {
  if (!logFilePath) {
    return;
  }

  try {
    rotateIfNeeded();
    const line = `${new Date().toISOString()} [${level}] ${args.map(formatArg).join(' ')}`;
    fs.appendFileSync(logFilePath, `${line}\n`, 'utf8');
  } catch {
    // Logging must never crash the agent.
  }
}

function rotateIfNeeded() {
  if (!fs.existsSync(logFilePath)) {
    return;
  }

  const stats = fs.statSync(logFilePath);
  if (stats.size < MAX_LOG_SIZE_BYTES) {
    return;
  }

  const rotatedName = `agent-${new Date().toISOString().replace(/[:.]/g, '-')}.log`;
  fs.renameSync(logFilePath, path.join(path.dirname(logFilePath), rotatedName));
}

function formatArg(arg) {
  if (arg instanceof Error) {
    return arg.stack || arg.message;
  }
  if (typeof arg === 'object') {
    try {
      return JSON.stringify(arg);
    } catch {
      return String(arg);
    }
  }
  return String(arg);
}

function getLogFilePath() {
  return logFilePath;
}

module.exports = { initialize, getLogFilePath };