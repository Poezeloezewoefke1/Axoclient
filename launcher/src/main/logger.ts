import { appendFile, mkdir } from 'node:fs/promises'
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
