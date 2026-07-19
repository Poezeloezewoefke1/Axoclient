import { Auth } from 'msmc'
import type { SessionInfo } from '../shared/types'

/**
 * Microsoft account login via msmc's Electron popup flow.
 *
 * Scaffold status: functional for development using msmc's default Azure
 * client ID. Remaining roadmap work:
 *  - P2-05: use our own client ID (AXO_MSA_CLIENT_ID from P0-02/P0-03)
 *  - P2-06: persist the refresh token with safeStorage (encrypted at rest)
 *  - P2-07: silent refresh on startup
 *  - P2-08: full error-case test matrix
 */

export interface AxoSession extends SessionInfo {
  /** Auth payload in the shape minecraft-launcher-core expects. Never send to the renderer. */
  mclcAuth: unknown
}

let currentSession: AxoSession | null = null

export async function loginWithMicrosoft(): Promise<AxoSession> {
  const authManager = new Auth('select_account')
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
