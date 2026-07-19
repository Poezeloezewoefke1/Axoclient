import { app, BrowserWindow, dialog, ipcMain, shell } from 'electron'
import { join } from 'node:path'
import { getManifest, getManifestInfo } from './manifest'
import { syncModsFolder } from './install'
import { getSession, initAuth, loginWithMicrosoft, logout, restoreSession } from './auth'
import { SettingsStore } from './settings'
import { launchGame } from './launch'
import { initLogger, logDirectory, logLine, writeCrashReport } from './logger'
import { initUpdater, installUpdate } from './updater'
import type { AxoSettings, GameProgress, SessionInfo } from '../shared/types'

function createWindow(): BrowserWindow {
  const win = new BrowserWindow({
    width: 1000,
    height: 640,
    minWidth: 900,
    minHeight: 600,
    show: false,
    autoHideMenuBar: true,
    backgroundColor: '#0a0a0f',
    title: 'Axo Launcher',
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      sandbox: false
    }
  })

  win.on('ready-to-show', () => win.show())

  // electron-vite sets ELECTRON_RENDERER_URL during `npm run dev`.
  if (process.env['ELECTRON_RENDERER_URL']) {
    void win.loadURL(process.env['ELECTRON_RENDERER_URL'])
  } else {
    void win.loadFile(join(__dirname, '../renderer/index.html'))
  }
  return win
}

function toSessionInfo(session: { username: string; uuid: string } | null): SessionInfo | null {
  return session ? { username: session.username, uuid: session.uuid } : null
}

let launching = false

// Crash reporter (P5-03, crash-handling.md case 1): report file + dialog,
// then a clean exit. No auto-upload — privacy-first.
process.on('uncaughtException', (error) => {
  const reportPath = writeCrashReport(error)
  try {
    dialog.showErrorBox(
      'Axo Launcher hit a problem',
      `${error.message}\n\n${reportPath ? `A report was saved to:\n${reportPath}` : 'A report could not be saved.'}`
    )
  } finally {
    app.exit(1)
  }
})
process.on('unhandledRejection', (reason) => {
  logLine('crash', `unhandled rejection: ${reason instanceof Error ? reason.stack : reason}`)
})

app.whenReady().then(async () => {
  initLogger(join(app.getPath('userData'), 'logs'))
  initAuth(join(app.getPath('userData'), 'auth.token'))
  logLine('app', `Axo Launcher ${app.getVersion()} starting`)

  const settings = new SettingsStore(join(app.getPath('userData'), 'settings.json'), {
    ramMb: 4096,
    channel: 'stable',
    installDir: join(app.getPath('appData'), '.axoclient'),
    jvmArgs: ''
  })
  await settings.load()

  ipcMain.handle('app:version', () => app.getVersion())
  ipcMain.handle('manifest:get', () => getManifestInfo())
  ipcMain.handle('auth:status', () => toSessionInfo(getSession()))
  ipcMain.handle('auth:restore', async () => toSessionInfo(await restoreSession()))
  ipcMain.handle('auth:login', async () => toSessionInfo(await loginWithMicrosoft()))
  ipcMain.handle('auth:logout', () => logout())
  ipcMain.handle('settings:get', () => settings.get())
  ipcMain.handle('settings:update', (_event, patch: Partial<AxoSettings>) =>
    settings.update(patch)
  )
  ipcMain.handle('game:launch', async (event, versionId: string) => {
    if (launching) {
      throw new Error('A launch is already in progress')
    }
    const session = getSession()
    if (!session) {
      throw new Error('Not signed in')
    }
    const sender = BrowserWindow.fromWebContents(event.sender)
    const report = (progress: GameProgress): void => {
      logLine('launch', `${progress.stage}${progress.detail ? ` ${progress.detail}` : ''}`)
      sender?.webContents.send('game:progress', progress)
    }
    launching = true
    try {
      const { manifest } = await getManifest()
      await launchGame(manifest, versionId, session, settings.get(), report)
    } catch (error) {
      logLine('launch', `failed: ${error instanceof Error ? error.message : error}`)
      throw error
    } finally {
      launching = false
    }
  })

  ipcMain.handle('update:install', () => installUpdate())
  // Repair (P3-05): full re-sync of the mods folder for the channel default
  // version — hash-mismatched files are re-downloaded, strays removed.
  ipcMain.handle('game:repair', async () => {
    const { manifest } = await getManifest()
    const current = settings.get()
    const channel = manifest.channels[current.channel] ?? Object.values(manifest.channels)[0]
    const version = channel?.versions.find((v) => v.id === channel.default)
    if (!version) {
      throw new Error('No installable version found in the manifest')
    }
    logLine('repair', `re-syncing ${version.id}`)
    const result = await syncModsFolder(current.installDir, version)
    logLine(
      'repair',
      `done: ${result.downloaded.length} downloaded, ${result.kept.length} kept, ${result.removed.length} removed`
    )
    return {
      downloaded: result.downloaded.length,
      kept: result.kept.length,
      removed: result.removed.length
    }
  })

  ipcMain.handle('logs:open', () => {
    const dir = logDirectory()
    if (dir) {
      void shell.openPath(dir)
    }
  })

  createWindow()
  initUpdater()

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow()
    }
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})
