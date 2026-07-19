import { appendFile, mkdir, readdir, rm } from 'node:fs/promises'
import { join } from 'node:path'

/**
 * Minimal timestamped file logger (start of P2-14). Every pipeline step
 * logs here so "Open logs" has something worth opening. Rotation lands
 * with the full error-surface task.
 */

let logDir: string | null = null

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
      void appendFile(path, `${line}\n`, 'utf8').catch(() => undefined)
    }
  }
}

export function logLine(scope: string, message: string): void {
  const line = `${new Date().toISOString()} [${scope}] ${message}\n`
  console.log(line.trim())
  const dir = logDir
  if (dir) {
    void mkdir(dir, { recursive: true })
      .then(() => appendFile(join(dir, 'launcher.log'), line, 'utf8'))
      .catch(() => undefined)
  }
}
