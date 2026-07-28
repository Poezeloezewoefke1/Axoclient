import { mkdir, readFile, rename, writeFile } from 'node:fs/promises'
import { dirname } from 'node:path'
import type { LaunchProfile } from '../shared/types'
import { SETTINGS_LIMITS } from './settings'

/**
 * Named launch profiles: one saved bundle of RAM / JVM args / channel /
 * version, so "PvP" and "modded singleplayer" stop fighting over one set of
 * settings.
 *
 * Same shape as settings.ts on purpose — plain JSON, atomic write, no
 * electron import — so the whole thing unit-tests without a browser.
 */

export const PROFILE_LIMITS = {
  maxNameLength: 40,
  maxProfiles: 20
} as const

/** Trim, collapse whitespace, cap length. Empty after that = not a name. */
export function normalizeName(raw: string): string {
  return raw.replace(/\s+/g, ' ').trim().slice(0, PROFILE_LIMITS.maxNameLength)
}

/** Case-insensitive: "PvP" and "pvp" are the same profile to a human. */
export function sameName(a: string, b: string): boolean {
  return a.toLowerCase() === b.toLowerCase()
}

function sanitizeProfile(raw: unknown): LaunchProfile | null {
  if (typeof raw !== 'object' || raw === null) return null
  const source = raw as Record<string, unknown>
  const name = typeof source.name === 'string' ? normalizeName(source.name) : ''
  if (name.length === 0) return null

  const ramMb =
    typeof source.ramMb === 'number' && Number.isFinite(source.ramMb)
      ? Math.min(SETTINGS_LIMITS.maxRamMb, Math.max(SETTINGS_LIMITS.minRamMb, Math.round(source.ramMb)))
      : undefined

  return {
    name,
    ramMb,
    jvmArgs: typeof source.jvmArgs === 'string' ? source.jvmArgs : undefined,
    channel:
      typeof source.channel === 'string' && source.channel.length > 0 ? source.channel : undefined,
    versionId:
      typeof source.versionId === 'string' && source.versionId.length > 0
        ? source.versionId
        : undefined
  }
}

/** Parse a profiles file; anything unreadable or malformed yields an empty list. */
export function parseProfiles(text: string): LaunchProfile[] {
  let raw: unknown
  try {
    raw = JSON.parse(text)
  } catch {
    return []
  }
  const list = Array.isArray(raw) ? raw : (raw as Record<string, unknown> | null)?.profiles
  if (!Array.isArray(list)) return []

  const out: LaunchProfile[] = []
  for (const entry of list) {
    const profile = sanitizeProfile(entry)
    // Drop duplicates rather than letting two "PvP"s shadow each other.
    if (profile && !out.some((p) => sameName(p.name, profile.name))) {
      out.push(profile)
    }
    if (out.length >= PROFILE_LIMITS.maxProfiles) break
  }
  return out
}

export function serializeProfiles(profiles: readonly LaunchProfile[]): string {
  return JSON.stringify({ profiles }, null, 2)
}

/** Add or replace by name. Returns a new list; the input is untouched. */
export function upsertProfile(
  profiles: readonly LaunchProfile[],
  profile: LaunchProfile
): LaunchProfile[] {
  const clean = sanitizeProfile(profile)
  if (!clean) return [...profiles]

  const index = profiles.findIndex((p) => sameName(p.name, clean.name))
  if (index >= 0) {
    const next = [...profiles]
    next[index] = clean
    return next
  }
  if (profiles.length >= PROFILE_LIMITS.maxProfiles) return [...profiles]
  return [...profiles, clean]
}

export function removeProfile(profiles: readonly LaunchProfile[], name: string): LaunchProfile[] {
  return profiles.filter((p) => !sameName(p.name, name))
}

export function findProfile(
  profiles: readonly LaunchProfile[],
  name: string
): LaunchProfile | undefined {
  return profiles.find((p) => sameName(p.name, name))
}

/**
 * Fields the profile actually sets, as a settings patch. Undefined fields are
 * left out entirely so a profile that only pins RAM doesn't wipe your JVM args.
 */
export function profilePatch(profile: LaunchProfile): Record<string, string | number> {
  const patch: Record<string, string | number> = {}
  if (profile.ramMb !== undefined) patch.ramMb = profile.ramMb
  if (profile.jvmArgs !== undefined) patch.jvmArgs = profile.jvmArgs
  if (profile.channel !== undefined) patch.channel = profile.channel
  return patch
}

export class ProfileStore {
  private profiles: LaunchProfile[] = []

  constructor(private readonly filePath: string) {}

  async load(): Promise<LaunchProfile[]> {
    try {
      this.profiles = parseProfiles(await readFile(this.filePath, 'utf8'))
    } catch {
      this.profiles = []
    }
    return this.list()
  }

  list(): LaunchProfile[] {
    return this.profiles.map((p) => ({ ...p }))
  }

  async save(profile: LaunchProfile): Promise<LaunchProfile[]> {
    this.profiles = upsertProfile(this.profiles, profile)
    await this.flush()
    return this.list()
  }

  async remove(name: string): Promise<LaunchProfile[]> {
    this.profiles = removeProfile(this.profiles, name)
    await this.flush()
    return this.list()
  }

  private async flush(): Promise<void> {
    await mkdir(dirname(this.filePath), { recursive: true })
    const temp = `${this.filePath}.tmp`
    await writeFile(temp, serializeProfiles(this.profiles), 'utf8')
    await rename(temp, this.filePath)
  }
}
