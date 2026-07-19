import { Auth } from 'msmc'
import type { SessionInfo } from '../shared/types'

/**
 * Microsoft account login via msmc's Electron popup flow.
 *
 * Scaffold status: functional for development using msmc's default Azure
 * client ID. Remaining roadmap work:
 *  - P0-03: flip USE_AXO_CLIENT_ID once Mojang approval lands
 *  - P2-06: persist the refresh token with safeStorage (encrypted at rest)
 *  - P2-07: silent refresh on startup
 *  - P2-08: full error-case test matrix
 */

export interface AxoSession extends SessionInfo {
  /** Auth payload in the shape minecraft-launcher-core expects. Never send to the renderer. */
  mclcAuth: unknown
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

let currentSession: AxoSession | null = null

export async function loginWithMicrosoft(): Promise<AxoSession> {
  const authManager = createAuthManager()
  const xboxManager = await authManager.launch('electron')
  const token = await xboxManager.getMinecraft()

  const session: AxoSession = {
    username: token.profile?.name ?? 'Player',
    uuid: token.profile?.id ?? '',
    mclcAuth: token.mclc()
  }
  currentSession = session
  return session
}

export function getSession(): AxoSession | null {
  return currentSession
}

export function logout(): void {
  // TODO(P2-06): also wipe the persisted refresh token.
  currentSession = null
}
