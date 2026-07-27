import { Auth } from 'msmc'
import { clearRefreshToken, loadRefreshToken, saveRefreshToken } from './tokens'
import { logLine } from './logger'
import {
  emptyStore,
  findAccount,
  parseStore,
  removeAccount as removeFromStore,
  serializeStore,
  toAccountInfos,
  upsertAccount,
  type AccountStore
} from './accountStore'
import type { AccountInfo, SessionInfo } from '../shared/types'

/**
 * Microsoft account login via msmc (roadmap P2-05..P2-08), with multi-account
 * support. Each account's refresh token is kept in an encrypted store; one
 * account is "active" (the one that launches). Switching accounts refreshes
 * that account's token silently (no popup); adding an account runs msmc's
 * Electron login popup.
 * - P0-03: flip USE_AXO_CLIENT_ID once Mojang approval lands.
 */

export interface AxoSession extends SessionInfo {
  /** Auth payload in the shape minecraft-launcher-core expects. Never send to the renderer. */
  mclcAuth: unknown
  /** Minecraft access token — used for skin changes. Never send to the renderer. */
  accessToken: string
}

/**
 * Azure app "Axo Launcher" (P0-02, registered 2026-07-19). Client IDs are
 * public by design — safe in source control. The redirect must match the
 * URI registered on the app ("Mobile and desktop applications").
 */
const AXO_MSA_CLIENT_ID = '77802178-1e6b-4163-b382-d4095833986e'

/**
 * Flip to true ONLY once Mojang approves the app for the Minecraft API
 * (P0-03). An unapproved ID passes the Microsoft/Xbox steps but is rejected
 * at the final Minecraft login, so we ride msmc's shared ID until then.
 */
const USE_AXO_CLIENT_ID = false

function createAuthManager(): Auth {
  if (USE_AXO_CLIENT_ID) {
    return new Auth({
      client_id: AXO_MSA_CLIENT_ID,
      redirect: 'http://localhost',
      prompt: 'select_account'
    })
  }
  return new Auth('select_account')
}

interface MinecraftLike {
  profile: { name: string; id: string } | undefined
  mclc(): unknown
  /** msmc exposes the raw Minecraft access token here. */
  mcToken?: string
}

let storeFile: string | null = null
let store: AccountStore = emptyStore()
let loaded = false
let currentSession: AxoSession | null = null

export function initAuth(filePath: string): void {
  storeFile = filePath
  loaded = false
  store = emptyStore()
}

async function ensureLoaded(): Promise<void> {
  if (loaded) {
    return
  }
  loaded = true
  if (!storeFile) {
    return
  }
  store = parseStore(await loadRefreshToken(storeFile))
}

async function persist(): Promise<void> {
  if (storeFile) {
    await saveRefreshToken(storeFile, serializeStore(store))
  }
}

function toSession(token: MinecraftLike): AxoSession {
  return {
    username: token.profile?.name ?? 'Player',
    uuid: token.profile?.id ?? '',
    mclcAuth: token.mclc(),
    accessToken: token.mcToken ?? ''
  }
}

function upsert(uuid: string, username: string, refresh: string): void {
  store = upsertAccount(store, { uuid, username, refresh })
}

/** Add a new account via the Microsoft popup, and make it active. */
export async function loginWithMicrosoft(): Promise<AxoSession> {
  await ensureLoaded()
  const authManager = createAuthManager()
  const xboxManager = await authManager.launch('electron')
  const token = await xboxManager.getMinecraft()

  currentSession = toSession(token)
  upsert(currentSession.uuid, currentSession.username, xboxManager.save())
  await persist()
  logLine('auth', `signed in as ${currentSession.username}`)
  return currentSession
}

/** Silent refresh of a stored account by uuid; updates the rotated token. */
async function activate(uuid: string): Promise<AxoSession> {
  const account = findAccount(store, uuid)
  if (!account) {
    throw new Error('Account not found — sign in again.')
  }
  const xboxManager = await createAuthManager().refresh(account.refresh)
  const token = await xboxManager.getMinecraft()
  currentSession = toSession(token)
  // Refresh tokens rotate — persist the newest one and keep names fresh.
  upsert(currentSession.uuid, currentSession.username, xboxManager.save())
  await persist()
  return currentSession
}

/** Silent startup restore (P2-07) of the active account. */
export async function restoreSession(): Promise<AxoSession | null> {
  await ensureLoaded()
  if (!store.activeUuid) {
    return null
  }
  try {
    const session = await activate(store.activeUuid)
    logLine('auth', `session restored for ${session.username}`)
    return session
  } catch (error) {
    logLine('auth', `silent refresh failed: ${error instanceof Error ? error.message : error}`)
    return null
  }
}

/** Switch the active account (silent refresh, no popup). */
export async function selectAccount(uuid: string): Promise<AxoSession> {
  await ensureLoaded()
  const session = await activate(uuid)
  logLine('auth', `switched to ${session.username}`)
  return session
}

export async function listAccounts(): Promise<AccountInfo[]> {
  await ensureLoaded()
  return toAccountInfos(store)
}

/**
 * Remove an account from the store. If it was the active one, fall back to
 * another stored account (silent refresh); returns the resulting session.
 */
export async function removeAccount(uuid: string): Promise<AxoSession | null> {
  await ensureLoaded()
  const wasActive = store.activeUuid === uuid
  store = removeFromStore(store, uuid)
  if (!wasActive) {
    await persist()
    return currentSession
  }
  // The removed account was the one playing — promote whoever the store
  // picked next (activate() re-persists with a rotated token).
  currentSession = null
  const nextUuid = store.activeUuid
  if (nextUuid) {
    try {
      return await activate(nextUuid)
    } catch {
      // Fall through: nothing usable left; the user signs in again.
    }
  }
  await persist()
  return currentSession
}

export function getSession(): AxoSession | null {
  return currentSession
}

/** "Sign out": remove the active account, falling back to the next if any. */
export async function logout(): Promise<SessionInfo | null> {
  await ensureLoaded()
  if (!store.activeUuid) {
    currentSession = null
    if (storeFile) {
      await clearRefreshToken(storeFile)
    }
    return null
  }
  const next = await removeAccount(store.activeUuid)
  logLine('auth', 'signed out')
  return next ? { username: next.username, uuid: next.uuid } : null
}
