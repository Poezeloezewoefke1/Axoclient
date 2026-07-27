import { app, BrowserWindow, dialog, ipcMain, shell } from 'electron'
import { join } from 'node:path'
import { getManifest, getManifestInfo } from './manifest'
import { syncModsFolder } from './install'
import {
  getSession,
  initAuth,
  listAccounts,
  loginWithMicrosoft,
  logout,
  removeAccount,
  restoreSession,
  selectAccount
} from './auth'
import { SettingsStore } from './settings'
import { forceCloseGame, launchGame } from './launch'
import { deleteVersion, listVersions } from './versions'
import { applySkin, getSkin, type SkinInfo } from './skin'
import { findLatestCrash } from './crashReport'
import { JVM_PRESETS, recommendedRamMb } from './system'
import { getNews } from './news'
import { addUserMods, listUserMods, removeUserMod, setUserModEnabled } from './userMods'
import { initLogger, logDirectory, logLine, readLauncherLog, writeCrashReport } from './logger'
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

/** Pipeline errors mapped to actionable copy (P2-14); originals go to launcher.log. */
function friendlyError(error: unknown): Error {
  const message = error instanceof Error ? error.message : String(error)
  if (/ENOTFOUND|EAI_AGAIN|ETIMEDOUT|ECONNR|fetch failed|network/i.test(message)) {
    return new Error('No internet connection — check your network and press Retry.')
  }
  if (/sha1 mismatch|sha256 mismatch/i.test(message)) {
    return new Error(
      'A downloaded file failed verification. Retry, or use Repair installation in Settings.'
    )
  }
  if (/HTTP 404/.test(message)) {
    return new Error(
      'A required file is not available online yet — the version manifest may be ahead of its release.'
    )
  }
  if (/HTTP (4|5)\d\d/.test(message)) {
    return new Error('A download server is having trouble — wait a minute and press Retry.')
  }
  return error instanceof Error ? error : new Error(message)
}

let launching = false
/** Start of the most recent launch — bounds the crash-report search. */
let lastLaunchAt = 0

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
  initAuth(join(app.getPath('userData'), 'accounts.dat'))
  logLine('app', `Axo Launcher ${app.getVersion()} starting`)

  const settings = new SettingsStore(join(app.getPath('userData'), 'settings.json'), {
    ramMb: 4096,
    channel: 'stable',
    installDir: join(app.getPath('appData'), '.axoclient'),
    jvmArgs: '',
    onboarded: false,
    playtimeMinutes: 0
  })
  await settings.load()

  ipcMain.handle('app:version', () => app.getVersion())
  ipcMain.handle('manifest:get', () => getManifestInfo())
  ipcMain.handle('auth:status', () => toSessionInfo(getSession()))
  ipcMain.handle('auth:restore', async () => toSessionInfo(await restoreSession()))
  ipcMain.handle('auth:login', async () => toSessionInfo(await loginWithMicrosoft()))
  ipcMain.handle('auth:logout', async () => await logout())
  ipcMain.handle('accounts:list', () => listAccounts())
  ipcMain.handle('accounts:select', async (_event, uuid: string) =>
    toSessionInfo(await selectAccount(uuid))
  )
  ipcMain.handle('accounts:remove', async (_event, uuid: string) =>
    toSessionInfo(await removeAccount(uuid))
  )
  // Player-owned mods, per version (protected from stray-removal on sync).
  ipcMain.handle('mods:list', (_event, versionId: string) =>
    listUserMods(settings.get().installDir, versionId)
  )
  ipcMain.handle('mods:add', async (_event, versionId: string) => {
    const picked = await dialog.showOpenDialog({
      title: 'Choose mod files',
      properties: ['openFile', 'multiSelections'],
      filters: [{ name: 'Minecraft mods', extensions: ['jar'] }]
    })
    if (picked.canceled || picked.filePaths.length === 0) {
      return listUserMods(settings.get().installDir, versionId)
    }
    return addUserMods(settings.get().installDir, versionId, picked.filePaths)
  })
  ipcMain.handle('mods:setEnabled', (_event, versionId: string, fileName: string, enabled: boolean) =>
    setUserModEnabled(settings.get().installDir, versionId, fileName, enabled)
  )
  ipcMain.handle('mods:remove', (_event, versionId: string, fileName: string) =>
    removeUserMod(settings.get().installDir, versionId, fileName)
  )
  ipcMain.handle('news:get', () => getNews())
  ipcMain.handle('system:recommendedRam', () => recommendedRamMb())
  ipcMain.handle('system:jvmPresets', () => JVM_PRESETS)
  ipcMain.handle('logs:read', () => readLauncherLog())
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
    lastLaunchAt = Date.now()
    try {
      const { manifest } = await getManifest()
      await launchGame(manifest, versionId, session, settings.get(), report)
      // Playtime is only credited for sessions that actually ran.
      const minutes = Math.round((Date.now() - lastLaunchAt) / 60_000)
      if (minutes > 0) {
        await settings.update({ playtimeMinutes: settings.get().playtimeMinutes + minutes })
      }
    } catch (error) {
      logLine('launch', `failed: ${error instanceof Error ? error.message : error}`)
      throw friendlyError(error)
    } finally {
      launching = false
    }
  })

  // Crash helper: diagnose the report the game just wrote, ignoring any
  // report older than the launch that produced it.
  ipcMain.handle('crash:latest', (_event, versionId: string) =>
    findLatestCrash(settings.get().installDir, versionId, lastLaunchAt)
  )

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

  ipcMain.handle('game:forceClose', () => forceCloseGame())

  // ---- Version management ----
  ipcMain.handle('versions:list', async () => {
    const { manifest } = await getManifest()
    return listVersions(settings.get().installDir, manifest)
  })

  ipcMain.handle('versions:install', async (event, versionId: string) => {
    const { manifest } = await getManifest()
    const version = Object.values(manifest.channels)
      .flatMap((c) => c.versions)
      .find((v) => v.id === versionId)
    if (!version) {
      throw friendlyError(new Error(`Unknown version id: ${versionId}`))
    }
    const sender = BrowserWindow.fromWebContents(event.sender)
    logLine('versions', `installing ${versionId}`)
    try {
      const result = await syncModsFolder(
        settings.get().installDir,
        version,
        undefined,
        (p) =>
          sender?.webContents.send('game:progress', {
            stage: 'mods',
            detail: p.file,
            received: p.received,
            total: p.total,
            bytesPerSecond: p.bytesPerSecond,
            etaSeconds: p.etaSeconds
          })
      )
      sender?.webContents.send('game:progress', { stage: 'closed' })
      return { downloaded: result.downloaded.length, kept: result.kept.length, removed: result.removed.length }
    } catch (error) {
      sender?.webContents.send('game:progress', { stage: 'closed' })
      logLine('versions', `install failed: ${error instanceof Error ? error.message : error}`)
      throw friendlyError(error)
    }
  })

  ipcMain.handle('versions:delete', async (_event, versionId: string) => {
    logLine('versions', `deleting ${versionId}`)
    await deleteVersion(settings.get().installDir, versionId)
  })

  ipcMain.handle('logs:open', () => {
    const dir = logDirectory()
    if (dir) {
      void shell.openPath(dir)
    }
  })

  ipcMain.handle('skin:get', async (): Promise<SkinInfo> => {
    const session = getSession()
    if (!session?.uuid) {
      return { dataUrl: null, slim: false }
    }
    return getSkin(session.uuid)
  })

  ipcMain.handle('skin:apply', async (_event, variant: 'classic' | 'slim'): Promise<string | null> => {
    const session = getSession()
    if (!session?.accessToken) {
      throw new Error('Sign in again to change your skin (no access token).')
    }
    const picked = await dialog.showOpenDialog({
      title: 'Choose a skin PNG (64×64)',
      properties: ['openFile'],
      filters: [{ name: 'Skin PNG', extensions: ['png'] }]
    })
    if (picked.canceled || picked.filePaths.length === 0) {
      return null
    }
    await applySkin(session.accessToken, picked.filePaths[0], variant)
    // Return the freshly-applied skin so the UI can update immediately.
    const refreshed = await getSkin(session.uuid)
    return refreshed.dataUrl
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
