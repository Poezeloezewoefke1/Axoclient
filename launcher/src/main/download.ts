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

export async function downloadFile(
  url: string,
  dest: string,
  expectedSha1?: string,
  options: DownloadOptions = {}
): Promise<void> {
  const attempts = options.attempts ?? 2
  const timeoutMs = options.timeoutMs ?? 60_000
  let lastError: Error = new Error('download not attempted')

  for (let attempt = 1; attempt <= attempts; attempt++) {
    try {
      const response = await fetch(url, { signal: AbortSignal.timeout(timeoutMs) })
      if (!response.ok) {
        throw new Error(`HTTP ${response.status} for ${url}`)
      }
      const bytes = Buffer.from(await response.arrayBuffer())
      if (expectedSha1) {
        const actual = createHash('sha1').update(bytes).digest('hex')
        if (actual !== expectedSha1) {
          throw new Error(`sha1 mismatch for ${url}: expected ${expectedSha1}, got ${actual}`)
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
