import { mkdir, readFile, rename, writeFile } from 'node:fs/promises'
import { dirname } from 'node:path'
import type { AxoSettings } from '../shared/types'

/**
 * Persistent launcher settings (roadmap P2-04). Plain JSON with atomic
 * writes — deliberately electron-free so it unit-tests without a browser;
 * index.ts supplies the electron-derived file path and defaults.
 */

export const SETTINGS_LIMITS = {
  minRamMb: 1024,
  maxRamMb: 16384
} as const

export class SettingsStore {
  private current: AxoSettings

  constructor(
    private readonly filePath: string,
    private readonly defaults: AxoSettings
  ) {
    this.current = { ...defaults }
  }

  /** Merge whatever is on disk onto the defaults; unreadable file = defaults. */
  async load(): Promise<AxoSettings> {
    try {
      const raw = JSON.parse(await readFile(this.filePath, 'utf8')) as unknown
      this.current = this.sanitize(raw)
    } catch {
      this.current = { ...this.defaults }
    }
    return this.get()
  }

  get(): AxoSettings {
    return { ...this.current }
  }

  async update(patch: Partial<AxoSettings>): Promise<AxoSettings> {
    this.current = this.sanitize({ ...this.current, ...patch })
    await this.save()
    return this.get()
  }

  private sanitize(raw: unknown): AxoSettings {
    const source = (typeof raw === 'object' && raw !== null ? raw : {}) as Record<string, unknown>
    const ramMb =
      typeof source.ramMb === 'number' && Number.isFinite(source.ramMb)
        ? Math.min(SETTINGS_LIMITS.maxRamMb, Math.max(SETTINGS_LIMITS.minRamMb, Math.round(source.ramMb)))
        : this.defaults.ramMb
    return {
      ramMb,
      channel:
        typeof source.channel === 'string' && source.channel.length > 0
          ? source.channel
          : this.defaults.channel,
      installDir:
        typeof source.installDir === 'string' && source.installDir.length > 0
          ? source.installDir
          : this.defaults.installDir,
      jvmArgs: typeof source.jvmArgs === 'string' ? source.jvmArgs : this.defaults.jvmArgs,
      onboarded: typeof source.onboarded === 'boolean' ? source.onboarded : this.defaults.onboarded,
      // Monotonic counter — never let a corrupt file wind playtime backwards.
      playtimeMinutes:
        typeof source.playtimeMinutes === 'number' &&
        Number.isFinite(source.playtimeMinutes) &&
        source.playtimeMinutes >= 0
          ? Math.floor(source.playtimeMinutes)
          : this.defaults.playtimeMinutes
    }
  }

  private async save(): Promise<void> {
    await mkdir(dirname(this.filePath), { recursive: true })
    const temp = `${this.filePath}.tmp`
    await writeFile(temp, JSON.stringify(this.current, null, 2), 'utf8')
    await rename(temp, this.filePath)
  }
}
