import { readdir, readFile, rm, stat } from 'node:fs/promises'
import { join } from 'node:path'
import { instanceDir } from './paths'
import type { ScreenshotInfo } from '../shared/types'

/**
 * Screenshots the game wrote for a version.
 *
 * Only metadata is listed — a folder of 4K PNGs would be tens of megabytes
 * as data URLs — and the image itself is read on demand when one is opened.
 */

/**
 * Filenames arrive from the renderer, so they must never be able to escape
 * the screenshots folder. Anything with a separator, a drive letter, or a
 * parent reference is rejected outright rather than normalised.
 */
export function isSafeScreenshotName(fileName: string): boolean {
  if (!fileName || fileName.length > 255) {
    return false
  }
  if (fileName.includes('/') || fileName.includes('\\') || fileName.includes('\0')) {
    return false
  }
  if (fileName === '.' || fileName === '..' || fileName.startsWith('..')) {
    return false
  }
  return /\.(png|jpg|jpeg)$/i.test(fileName)
}

export function screenshotsDir(installDir: string, versionId: string): string {
  return join(instanceDir(installDir, versionId), 'screenshots')
}

/** Newest first, so the shot you just took is the one you see. */
export async function listScreenshots(
  installDir: string,
  versionId: string
): Promise<ScreenshotInfo[]> {
  const dir = screenshotsDir(installDir, versionId)
  try {
    const names = (await readdir(dir)).filter(isSafeScreenshotName)
    const infos = await Promise.all(
      names.map(async (fileName) => {
        const stats = await stat(join(dir, fileName))
        return { fileName, sizeBytes: stats.size, modifiedAt: stats.mtimeMs }
      })
    )
    return infos.sort((a, b) => b.modifiedAt - a.modifiedAt)
  } catch {
    return []
  }
}

/** The image itself, as a data URL so it renders under the strict CSP. */
export async function readScreenshot(
  installDir: string,
  versionId: string,
  fileName: string
): Promise<string | null> {
  if (!isSafeScreenshotName(fileName)) {
    return null
  }
  try {
    const bytes = await readFile(join(screenshotsDir(installDir, versionId), fileName))
    const mime = /\.png$/i.test(fileName) ? 'image/png' : 'image/jpeg'
    return `data:${mime};base64,${bytes.toString('base64')}`
  } catch {
    return null
  }
}

export async function deleteScreenshot(
  installDir: string,
  versionId: string,
  fileName: string
): Promise<ScreenshotInfo[]> {
  if (isSafeScreenshotName(fileName)) {
    await rm(join(screenshotsDir(installDir, versionId), fileName), { force: true })
  }
  return listScreenshots(installDir, versionId)
}

/** Absolute path, for revealing in the OS file manager. */
export function screenshotPath(
  installDir: string,
  versionId: string,
  fileName: string
): string | null {
  return isSafeScreenshotName(fileName)
    ? join(screenshotsDir(installDir, versionId), fileName)
    : null
}
