import { appendFile, mkdir, readdir, readFile, rm } from 'node:fs/promises'
import { mkdirSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Minimal timestamped file logger (start of P2-14). Every pipeline step
 * logs here so "Open logs" has something worth opening. Rotation lands
 * with the full error-surface task.
 */

let logDir: string | null = null

/**
 * Strip credentials before anything reaches disk. MCLC echoes the full
 * java command line on its `debug` channel, which includes the live
 * Minecraft access token — logs get shared when people report crashes,
 * so the token must never land in the file. Applied centrally in the two
 * write paths rather than at call sites, so a new logLine() caller can't
 * reintroduce the leak.
 */
export function redactSecrets(text: string): string {
  return (
    text
      // --accessToken <jwt>, --clientId <id>, --xuid <id>, --uuid <id>
      .replace(/(--(?:accessToken|clientId|xuid|uuid)[ =])\S+/g, '$1<redacted>')
      // Authorization: Bearer <token>
      .replace(/(Bearer\s+)[\w-]+\.[\w-]+\.[\w-]+/gi, '$1<redacted>')
      // Any bare JWT that slipped through another format
      .replace(/eyJ[\w-]{8,}\.[\w-]+\.[\w-]+/g, '<redacted-jwt>')
  )
}

export function initLogger(dir: string): void {
  logDir = dir
}

export function logDirectory(): string | null {
  return logDir
}

/**
 * Per-session game log (P5-02): one file per launch, oldest pruned so at
 * most `keep` remain. Game stdout goes here, not into launcher.log.
 */
export function createGameLog(keep = 5): { path: string | null; append: (line: string) => void } {
  const dir = logDir
  if (!dir) {
    return { path: null, append: () => undefined }
  }
  const stamp = new Date().toISOString().replace(/[:.]/g, '-')
  const path = join(dir, `game-${stamp}.log`)

  void mkdir(dir, { recursive: true })
    .then(async () => {
      const games = (await readdir(dir)).filter((f) => f.startsWith('game-')).sort()
      for (const old of games.slice(0, Math.max(0, games.length - (keep - 1)))) {
        await rm(join(dir, old), { force: true })
      }
    })
    .catch(() => undefined)

  return {
    path,
    append: (line: string) => {
      void appendFile(path, `${redactSecrets(line)}\n`, 'utf8').catch(() => undefined)
    }
  }
}

/**
 * Crash report file (P5-03). Synchronous on purpose — called while the
 * process is dying, when async work may never flush.
 */
export function writeCrashReport(error: Error): string | null {
  const dir = logDir
  if (!dir) {
    return null
  }
  try {
    mkdirSync(dir, { recursive: true })
    const path = join(dir, `crash-${new Date().toISOString().replace(/[:.]/g, '-')}.txt`)
    writeFileSync(path, `${new Date().toISOString()}\n${error.stack ?? error.message}\n`, 'utf8')
    return path
  } catch {
    return null
  }
}

/**
 * Tail of launcher.log for the in-app viewer. Only the last `maxLines` are
 * returned so a long-running install can't flood the renderer.
 */
export async function readLauncherLog(maxLines = 400): Promise<string> {
  const dir = logDir
  if (!dir) {
    return ''
  }
  try {
    const text = await readFile(join(dir, 'launcher.log'), 'utf8')
    return text.split('\n').slice(-maxLines).join('\n').trim()
  } catch {
    return ''
  }
}

export function logLine(scope: string, message: string): void {
  const line = `${new Date().toISOString()} [${scope}] ${redactSecrets(message)}\n`
  console.log(line.trim())
  const dir = logDir
  if (dir) {
    void mkdir(dir, { recursive: true })
      .then(() => appendFile(join(dir, 'launcher.log'), line, 'utf8'))
      .catch(() => undefined)
  }
}
