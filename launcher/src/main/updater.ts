import { app, BrowserWindow } from 'electron'
import { autoUpdater } from 'electron-updater'
import { logLine } from './logger'
import type { UpdateStatus } from '../shared/types'

/**
 * Launcher self-update via GitHub Releases (roadmap P3-02). electron-updater
 * reads the feed configured in electron-builder.yml (publish: github) using
 * the latest.yml asset each release ships. Checks on startup and every 4 h;
 * downloads happen in the background and the UI only hears about a
 * ready-to-install update.
 */

const CHECK_INTERVAL_MS = 4 * 60 * 60 * 1000

function broadcast(status: UpdateStatus): void {
  for (const win of BrowserWindow.getAllWindows()) {
    win.webContents.send('update:status', status)
  }
}

export function initUpdater(): void {
  if (!app.isPackaged) {
    logLine('updater', 'dev build — update checks disabled')
    return
  }

  autoUpdater.autoDownload = true
  autoUpdater.on('update-downloaded', (info) => {
    logLine('updater', `update ${info.version} downloaded`)
    broadcast({ state: 'ready', version: info.version })
  })
  autoUpdater.on('error', (error) => {
    // Never bother the user about a failed background check.
    logLine('updater', `check failed: ${error.message}`)
  })

  const check = (): void => {
    void autoUpdater.checkForUpdates().catch(() => undefined)
  }
  check()
  setInterval(check, CHECK_INTERVAL_MS)
}

export function installUpdate(): void {
  logLine('updater', 'restarting to install update')
  autoUpdater.quitAndInstall()
}
