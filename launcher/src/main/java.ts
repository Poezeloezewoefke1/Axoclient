import { mkdir, readFile, readdir, rm, stat, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import extract from 'extract-zip'
import { z } from 'zod'
import { downloadFile } from './download'

/**
 * Java runtime provisioning via the Adoptium API (roadmap P2-09):
 * query the latest Temurin JRE for the manifest's javaMajor, download
 * the zip (sha256-verified), extract into the launcher-owned runtime
 * directory, and cache the resolved java path in a marker file so the
 * second run is a no-op. Network calls are injectable for tests.
 */

const assetsSchema = z
  .array(
    z.object({
      release_name: z.string(),
      binary: z.object({
        package: z.object({
          link: z.string().url(),
          checksum: z.string().regex(/^[0-9a-f]{64}$/)
        })
      })
    })
  )
  .min(1)

export interface JreBinary {
  url: string
  sha256: string
  releaseName: string
}

export function adoptiumAssetsUrl(major: number, os = 'windows', arch = 'x64'): string {
  return `https://api.adoptium.net/v3/assets/latest/${major}/hotspot?os=${os}&architecture=${arch}&image_type=jre&vendor=eclipse`
}

export function pickBinary(apiResponse: unknown): JreBinary {
  const assets = assetsSchema.parse(apiResponse)
  const first = assets[0]
  return {
    url: first.binary.package.link,
    sha256: first.binary.package.checksum,
    releaseName: first.release_name
  }
}

async function defaultFetchJson(url: string): Promise<unknown> {
  const response = await fetch(url, { signal: AbortSignal.timeout(30_000) })
  if (!response.ok) {
    throw new Error(`HTTP ${response.status} for ${url}`)
  }
  return response.json()
}

export interface EnsureJavaOptions {
  /** Injectable for tests; defaults to a fetch of the Adoptium API. */
  fetchJson?: (url: string) => Promise<unknown>
  /** Relative path of the java executable inside the extracted JRE root. */
  exeRelPath?: string
  os?: string
  arch?: string
  onProgress?: (stage: 'cached' | 'query' | 'download' | 'extract', detail?: string) => void
}

interface JreMarker {
  releaseName: string
  javaPath: string
}

/**
 * Returns the absolute path to the java executable for `major`, provisioning
 * it if missing. `runtimeRoot` must be absolute (extract-zip requirement).
 */
export async function ensureJava(
  major: number,
  runtimeRoot: string,
  options: EnsureJavaOptions = {}
): Promise<string> {
  const dir = join(runtimeRoot, String(major))
  const marker = join(dir, 'axo-jre.json')
  const exeRelPath = options.exeRelPath ?? join('bin', 'javaw.exe')

  try {
    const cached = JSON.parse(await readFile(marker, 'utf8')) as Partial<JreMarker>
    if (cached.javaPath && (await stat(cached.javaPath)).isFile()) {
      options.onProgress?.('cached', cached.javaPath)
      return cached.javaPath
    }
  } catch {
    // No usable cache — provision below.
  }

  options.onProgress?.('query')
  const fetchJson = options.fetchJson ?? defaultFetchJson
  const binary = pickBinary(await fetchJson(adoptiumAssetsUrl(major, options.os, options.arch)))

  await rm(dir, { recursive: true, force: true })
  await mkdir(dir, { recursive: true })
  const zipPath = join(dir, 'jre.zip')
  options.onProgress?.('download', binary.releaseName)
  await downloadFile(binary.url, zipPath, { algorithm: 'sha256', value: binary.sha256 })
  options.onProgress?.('extract', binary.releaseName)
  await extract(zipPath, { dir })
  await rm(zipPath, { force: true })

  const entries = await readdir(dir, { withFileTypes: true })
  const root = entries.find((entry) => entry.isDirectory())
  if (!root) {
    throw new Error(`JRE archive contained no directory (release ${binary.releaseName})`)
  }
  const javaPath = join(dir, root.name, exeRelPath)
  await stat(javaPath) // fail loudly if the executable is not where expected
  const markerData: JreMarker = { releaseName: binary.releaseName, javaPath }
  await writeFile(marker, JSON.stringify(markerData, null, 2), 'utf8')
  return javaPath
}
