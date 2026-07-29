import { mkdir, readFile, rename, writeFile } from 'node:fs/promises'
import { dirname } from 'node:path'
import type { SavedServer } from '../shared/types'

/**
 * Saved servers for the quick-join box, so a favourite address is a click
 * rather than something to retype every launch.
 *
 * Pure logic plus an atomic-write store, same shape as settings.ts and
 * profiles.ts — no electron import, so it unit-tests directly.
 */

export const SERVER_LIMITS = {
  maxNameLength: 40,
  maxAddressLength: 253,
  maxServers: 30
} as const

/**
 * Trim and lowercase the host, keeping an explicit :port.
 *
 * Deliberately permissive about *what* the host is — LAN names, IPv4 and
 * Hamachi-style addresses are all legitimate and the game is the real
 * authority on whether one resolves. This only rejects text that cannot be
 * an address at all.
 */
export function normalizeAddress(raw: string): string {
  return raw.trim().replace(/\s+/g, '').toLowerCase().slice(0, SERVER_LIMITS.maxAddressLength)
}

/** Rejects empty strings and anything carrying a scheme, path or credentials. */
export function isValidAddress(raw: string): boolean {
  const address = normalizeAddress(raw)
  if (address.length === 0) return false
  // A pasted URL is the common mistake; say no rather than pass "https://..."
  // to the game where it fails with something unhelpful.
  if (/[/\\@?#]/.test(address) || address.includes('://')) return false

  const [host, port, ...rest] = address.split(':')
  if (rest.length > 0) return false
  if (host.length === 0) return false
  if (port !== undefined) {
    if (!/^\d{1,5}$/.test(port)) return false
    const value = Number(port)
    if (value < 1 || value > 65535) return false
  }
  return /^[a-z0-9.\-_]+$/.test(host)
}

export function normalizeName(raw: string): string {
  return raw.replace(/\s+/g, ' ').trim().slice(0, SERVER_LIMITS.maxNameLength)
}

function sanitizeServer(raw: unknown): SavedServer | null {
  if (typeof raw !== 'object' || raw === null) return null
  const source = raw as Record<string, unknown>
  const address = typeof source.address === 'string' ? normalizeAddress(source.address) : ''
  if (!isValidAddress(address)) return null

  const name = typeof source.name === 'string' ? normalizeName(source.name) : ''
  return {
    // An unnamed entry still deserves a label; the address is the honest one.
    name: name.length > 0 ? name : address,
    address
  }
}

export function parseServers(text: string): SavedServer[] {
  let raw: unknown
  try {
    raw = JSON.parse(text)
  } catch {
    return []
  }
  const list = Array.isArray(raw) ? raw : (raw as Record<string, unknown> | null)?.servers
  if (!Array.isArray(list)) return []

  const out: SavedServer[] = []
  for (const entry of list) {
    const server = sanitizeServer(entry)
    // Keyed by address: two entries for the same host are one favourite.
    if (server && !out.some((s) => s.address === server.address)) {
      out.push(server)
    }
    if (out.length >= SERVER_LIMITS.maxServers) break
  }
  return out
}

export function serializeServers(servers: readonly SavedServer[]): string {
  return JSON.stringify({ servers }, null, 2)
}

/** Add, or rename in place when the address is already saved. */
export function upsertServer(
  servers: readonly SavedServer[],
  server: SavedServer
): SavedServer[] {
  const clean = sanitizeServer(server)
  if (!clean) return [...servers]

  const index = servers.findIndex((s) => s.address === clean.address)
  if (index >= 0) {
    const next = [...servers]
    next[index] = clean
    return next
  }
  if (servers.length >= SERVER_LIMITS.maxServers) return [...servers]
  return [...servers, clean]
}

export function removeServer(servers: readonly SavedServer[], address: string): SavedServer[] {
  const target = normalizeAddress(address)
  return servers.filter((s) => s.address !== target)
}

export class ServerStore {
  private servers: SavedServer[] = []

  constructor(private readonly filePath: string) {}

  async load(): Promise<SavedServer[]> {
    try {
      this.servers = parseServers(await readFile(this.filePath, 'utf8'))
    } catch {
      this.servers = []
    }
    return this.list()
  }

  list(): SavedServer[] {
    return this.servers.map((s) => ({ ...s }))
  }

  async save(server: SavedServer): Promise<SavedServer[]> {
    this.servers = upsertServer(this.servers, server)
    await this.flush()
    return this.list()
  }

  async remove(address: string): Promise<SavedServer[]> {
    this.servers = removeServer(this.servers, address)
    await this.flush()
    return this.list()
  }

  private async flush(): Promise<void> {
    await mkdir(dirname(this.filePath), { recursive: true })
    const temp = `${this.filePath}.tmp`
    await writeFile(temp, serializeServers(this.servers), 'utf8')
    await rename(temp, this.filePath)
  }
}
