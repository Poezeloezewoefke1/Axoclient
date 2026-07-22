import { Auth } from 'msmc'
import { clearRefreshToken, loadRefreshToken, saveRefreshToken } from './tokens'
import { logLine } from './logger'
import type { SessionInfo } from '../shared/types'

/**
 * Microsoft account login via msmc (roadmap P2-05..P2-08).
 * - Login uses msmc's Electron popup flow.
 * - The refresh token is persisted encrypted (tokens.ts) and used for a
 *   silent restore on startup.
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

let tokenFile: string | null = null
let currentSession: AxoSession | null = null

export function initAuth(tokenFilePath: string): void {
  tokenFile = tokenFilePath
}

function toSession(token: MinecraftLike): AxoSession {
  return {
    username: token.profile?.name ?? 'Player',
    uuid: token.profile?.id ?? '',
    mclcAuth: token.mclc(),
    accessToken: token.mcToken ?? ''
  }
}

export async function loginWithMicrosoft(): Promise<AxoSession> {
  const authManager = createAuthManager()
  const xboxManager = await authManager.launch('electron')
  const token = await xboxManager.getMinecraft()

  currentSession = toSession(token)
  if (tokenFile) {
    await saveRefreshToken(tokenFile, xboxManager.save())
  }
  logLine('auth', `signed in as ${currentSession.username}`)
  return currentSession
}

/** Silent startup restore (P2-07). Returns null when re-login is required. */
export async function restoreSession(): Promise<AxoSession | null> {
  if (!tokenFile) {
    return null
  }
  const stored = await loadRefreshToken(tokenFile)
  if (!stored) {
    return null
  }
  try {
    const xboxManager = await createAuthManager().refresh(stored)
    const token = await xboxManager.getMinecraft()
    currentSession = toSession(token)
    // Refresh tokens rotate — persist the newest one.
    await saveRefreshToken(tokenFile, xboxManager.save())
    logLine('auth', `session restored for ${currentSession.username}`)
    return currentSession
  } catch (error) {
    logLine('auth', `silent refresh failed: ${error instanceof Error ? error.message : error}`)
    await clearRefreshToken(tokenFile)
    return null
  }
}

export function getSession(): AxoSession | null {
  return currentSession
}

export async function logout(): Promise<void> {
  currentSession = null
  if (tokenFile) {
    await clearRefreshToken(tokenFile)
  }
  logLine('auth', 'signed out')
}
