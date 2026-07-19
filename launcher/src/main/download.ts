import { createHash } from 'node:crypto'
import { mkdir, readFile, rename, rm, writeFile } from 'node:fs/promises'
import { dirname } from 'node:path'

/**
 * Hash-verified downloads — the trust boundary of the install pipeline
 * (roadmap P2-11/P2-12). Every file the launcher fetches lands through
 * here; a sha1 mismatch is deleted and retried, never kept.
 */

export async function sha1File(path: string): Promise<string> {
  return createHash('sha1').update(await readFile(path)).digest('hex')
}

export interface DownloadOptions {
  /** Total attempts including the first (default 2 = one retry). */
  attempts?: number
  timeoutMs?: number
}

/** A bare string means sha1 (the manifest's hash); Adoptium et al. use sha256. */
export type ExpectedHash = string | { algorithm: 'sha1' | 'sha256'; value: string }

export async function downloadFile(
  url: string,
  dest: string,
  expectedHash?: ExpectedHash,
  options: DownloadOptions = {}
): Promise<void> {
  const attempts = options.attempts ?? 2
  const timeoutMs = options.timeoutMs ?? 60_000
  const expected =
    typeof expectedHash === 'string' ? { algorithm: 'sha1' as const, value: expectedHash } : expectedHash
  let lastError: Error = new Error('download not attempted')

  for (let attempt = 1; attempt <= attempts; attempt++) {
    try {
      const response = await fetch(url, { signal: AbortSignal.timeout(timeoutMs) })
      if (!response.ok) {
        throw new Error(`HTTP ${response.status} for ${url}`)
      }
      const bytes = Buffer.from(await response.arrayBuffer())
      if (expected) {
        const actual = createHash(expected.algorithm).update(bytes).digest('hex')
        if (actual !== expected.value) {
          throw new Error(
            `${expected.algorithm} mismatch for ${url}: expected ${expected.value}, got ${actual}`
          )
        }
      }
      await mkdir(dirname(dest), { recursive: true })
      const temp = `${dest}.part`
      await writeFile(temp, bytes)
      await rename(temp, dest)
      return
    } catch (error) {
      lastError = error instanceof Error ? error : new Error(String(error))
      await rm(`${dest}.part`, { force: true })
    }
  }
  throw lastError
}
