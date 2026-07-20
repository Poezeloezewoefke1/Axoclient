import { mkdir, readdir, rm } from 'node:fs/promises'
import { join } from 'node:path'
import { downloadFile, sha1File } from './download'
import { RateTracker } from './progress'

/**
 * Reconciles a launcher-owned directory with a desired file list
 * (roadmap P2-11): download what's missing or corrupted, keep what
 * matches, delete stray jars. Idempotent — re-running on a correct
 * directory does nothing. Only *.jar strays are removed so configs
 * and logs are never touched.
 */

export interface DesiredFile {
  fileName: string
  url: string
  sha1: string
}

export interface SyncEvent {
  file: string
  action: 'download' | 'keep' | 'remove'
}

/** Live transfer stats for the file currently downloading. */
export interface SyncProgress {
  file: string
  received: number
  total: number | undefined
  bytesPerSecond: number
  etaSeconds: number | null
}

export interface SyncResult {
  downloaded: string[]
  kept: string[]
  removed: string[]
}

export async function syncDirectory(
  dir: string,
  desired: DesiredFile[],
  onEvent?: (event: SyncEvent) => void,
  onProgress?: (progress: SyncProgress) => void
): Promise<SyncResult> {
  await mkdir(dir, { recursive: true })
  const result: SyncResult = { downloaded: [], kept: [], removed: [] }
  const existing = new Set(await readdir(dir))
  const wanted = new Set(desired.map((f) => f.fileName))

  for (const file of desired) {
    const path = join(dir, file.fileName)
    let needsDownload = true
    if (existing.has(file.fileName)) {
      needsDownload = (await sha1File(path)) !== file.sha1
    }
    if (needsDownload) {
      onEvent?.({ file: file.fileName, action: 'download' })
      const tracker = new RateTracker()
      await downloadFile(file.url, path, file.sha1, {
        onProgress: onProgress
          ? (p) => {
              const now = Date.now()
              tracker.update(p.received, now)
              onProgress({
                file: file.fileName,
                received: p.received,
                total: p.total,
                bytesPerSecond: tracker.bytesPerSecond(),
                etaSeconds: tracker.etaSeconds(p.total)
              })
            }
          : undefined
      })
      result.downloaded.push(file.fileName)
    } else {
      onEvent?.({ file: file.fileName, action: 'keep' })
      result.kept.push(file.fileName)
    }
  }

  for (const name of existing) {
    if (name.endsWith('.jar') && !wanted.has(name)) {
      onEvent?.({ file: name, action: 'remove' })
      await rm(join(dir, name), { force: true })
      result.removed.push(name)
    }
  }

  return result
}
