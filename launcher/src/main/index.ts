import { app, BrowserWindow, ipcMain } from 'electron'
import { join } from 'node:path'
import { getManifestInfo } from './manifest'
import { getSession, loginWithMicrosoft, logout } from './auth'
import { SettingsStore } from './settings'
import type { AxoSettings, SessionInfo } from '../shared/types'

function createWindow(): void {
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
}

function toSessionInfo(session: { username: string; uuid: string } | null): SessionInfo | null {
  return session ? { username: session.username, uuid: session.uuid } : null
}

app.whenReady().then(async () => {
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
  ipcMain.handle('auth:login', async () => toSessionInfo(await loginWithMicrosoft()))
  ipcMain.handle('auth:logout', () => logout())
  ipcMain.handle('settings:get', () => settings.get())
  ipcMain.handle('settings:update', (_event, patch: Partial<AxoSettings>) =>
    settings.update(patch)
  )
  // game:launch IPC lands with the full pipeline in P2-13 (see launch.ts, install.ts).

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
