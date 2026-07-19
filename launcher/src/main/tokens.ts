import { safeStorage } from 'electron'
import { mkdir, readFile, rm, writeFile } from 'node:fs/promises'
import { dirname } from 'node:path'
import { logLine } from './logger'

/**
 * Refresh-token persistence (roadmap P2-06). The token is encrypted with
 * Electron safeStorage (DPAPI on Windows) — never written as plaintext.
 * If OS-level encryption is unavailable we simply don't persist, and the
 * user signs in again next start.
 */

export async function saveRefreshToken(filePath: string, token: string): Promise<void> {
  if (!safeStorage.isEncryptionAvailable()) {
    logLine('auth', 'safeStorage unavailable — session will not survive restarts')
    return
  }
  await mkdir(dirname(filePath), { recursive: true })
  await writeFile(filePath, safeStorage.encryptString(token))
}

export async function loadRefreshToken(filePath: string): Promise<string | null> {
  try {
    if (!safeStorage.isEncryptionAvailable()) {
      return null
    }
    return safeStorage.decryptString(await readFile(filePath))
  } catch {
    return null
  }
}

export async function clearRefreshToken(filePath: string): Promise<void> {
  await rm(filePath, { force: true })
}
