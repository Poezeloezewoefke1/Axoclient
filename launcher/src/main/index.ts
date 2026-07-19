import { app, BrowserWindow, ipcMain } from 'electron'
import { join } from 'node:path'
import { getManifest, getManifestInfo } from './manifest'
import { getSession, initAuth, loginWithMicrosoft, logout, restoreSession } from './auth'
import { SettingsStore } from './settings'
import { launchGame } from './launch'
import { initLogger, logLine } from './logger'
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

  createWindow()

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
