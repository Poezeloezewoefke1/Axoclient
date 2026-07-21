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

export async function fetchJson(url: string, timeoutMs = 30_000): Promise<unknown> {
  const response = await fetch(url, { signal: AbortSignal.timeout(timeoutMs) })
  if (!response.ok) {
    throw new Error(`HTTP ${response.status} for ${url}`)
  }
  return response.json()
}

/** Streamed progress for a single file: received / total bytes so far. */
export interface FileProgress {
  received: number
  /** Total size from Content-Length, or undefined when the server omits it. */
  total: number | undefined
}

export interface DownloadOptions {
  /** Total attempts including the first (default 2 = one retry). */
  attempts?: number
  timeoutMs?: number
  /** Base backoff before the first retry; doubles each attempt (default 500ms). */
  retryDelayMs?: number
  /** Called as bytes arrive (throttled by the caller if needed). */
  onProgress?: (progress: FileProgress) => void
  /** Observability hook fired before each backoff sleep (used by tests/UI). */
  onRetry?: (info: { attempt: number; delayMs: number; error: Error }) => void
}

const sleep = (ms: number): Promise<void> => new Promise((resolve) => setTimeout(resolve, ms))

/** A bare string means sha1 (the manifest's hash); Adoptium et al. use sha256. */
export type ExpectedHash = string | { algorithm: 'sha1' | 'sha256'; value: string }

/** Read a fetch body stream into one buffer, reporting cumulative progress. */
async function drainBody(
  response: Response,
  total: number | undefined,
  onProgress?: (p: FileProgress) => void
): Promise<Buffer> {
  const body = response.body
  if (!body) {
    // No stream available — fall back to a single buffered read.
    const bytes = Buffer.from(await response.arrayBuffer())
    onProgress?.({ received: bytes.length, total: total ?? bytes.length })
    return bytes
  }
  const reader = body.getReader()
  const chunks: Buffer[] = []
  let received = 0
  for (;;) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }
    const chunk = Buffer.from(value)
    chunks.push(chunk)
    received += chunk.length
    onProgress?.({ received, total })
  }
  return Buffer.concat(chunks)
}

export async function downloadFile(
  url: string,
  dest: string,
  expectedHash?: ExpectedHash,
  options: DownloadOptions = {}
): Promise<void> {
  const attempts = options.attempts ?? 2
  const timeoutMs = options.timeoutMs ?? 60_000
  const retryDelayMs = options.retryDelayMs ?? 500
  const expected =
    typeof expectedHash === 'string' ? { algorithm: 'sha1' as const, value: expectedHash } : expectedHash
  let lastError: Error = new Error('download not attempted')

  for (let attempt = 1; attempt <= attempts; attempt++) {
    try {
      const response = await fetch(url, { signal: AbortSignal.timeout(timeoutMs) })
      if (!response.ok) {
        throw new Error(`HTTP ${response.status} for ${url}`)
      }
      const lenHeader = response.headers.get('content-length')
      const total = lenHeader ? Number(lenHeader) : undefined
      const bytes = await drainBody(response, total, options.onProgress)
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
      if (attempt < attempts) {
        // Exponential backoff so transient 503s / rate limits get room to
        // recover instead of hammering the CDN with instant retries.
        const delayMs = retryDelayMs * 2 ** (attempt - 1)
        options.onRetry?.({ attempt, delayMs, error: lastError })
        await sleep(delayMs)
      }
    }
  }
  throw lastError
}
