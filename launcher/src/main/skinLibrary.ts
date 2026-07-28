import { copyFile, mkdir, readdir, readFile, rm, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { logLine } from './logger'
import type { SavedSkin } from '../shared/types'

/**
 * A local wardrobe of skins. Changing your skin hits Mojang and takes a
 * moment to propagate, so people keep re-uploading the same few files;
 * saving them here makes swapping a click instead of a file hunt.
 *
 * Skins live as plain PNGs in a folder with a small JSON index holding the
 * display name and arm style. The PNG is the source of truth — an index
 * entry with no file is dropped on read.
 */

const INDEX_FILE = 'skins.json'

interface IndexEntry {
  id: string
  name: string
  slim: boolean
}

/**
 * Make a filesystem-safe id from a display name. Collisions are the caller's
 * problem to resolve (see `uniqueId`) — this only guarantees safety.
 */
export function toSkinId(name: string): string {
  const cleaned = name
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 40)
  return cleaned.length > 0 ? cleaned : 'skin'
}

/** First free id in the form base, base-2, base-3… */
export function uniqueId(base: string, taken: ReadonlySet<string>): string {
  if (!taken.has(base)) {
    return base
  }
  let counter = 2
  while (taken.has(`${base}-${counter}`)) {
    counter += 1
  }
  return `${base}-${counter}`
}

function indexPath(dir: string): string {
  return join(dir, INDEX_FILE)
}

export function parseIndex(raw: string | null): IndexEntry[] {
  if (!raw) {
    return []
  }
  try {
    const parsed = JSON.parse(raw) as unknown
    if (!Array.isArray(parsed)) {
      return []
    }
    return parsed.filter(
      (e): e is IndexEntry =>
        typeof e === 'object' &&
        e !== null &&
        typeof (e as IndexEntry).id === 'string' &&
        (e as IndexEntry).id.length > 0 &&
        typeof (e as IndexEntry).name === 'string'
    ).map((e) => ({ id: e.id, name: e.name, slim: Boolean(e.slim) }))
  } catch {
    return []
  }
}

async function readIndex(dir: string): Promise<IndexEntry[]> {
  try {
    return parseIndex(await readFile(indexPath(dir), 'utf8'))
  } catch {
    return []
  }
}

async function writeIndex(dir: string, entries: IndexEntry[]): Promise<void> {
  await mkdir(dir, { recursive: true })
  await writeFile(indexPath(dir), JSON.stringify(entries, null, 2), 'utf8')
}

/** Saved skins, each with its PNG inlined so the renderer can draw it. */
export async function listSavedSkins(dir: string): Promise<SavedSkin[]> {
  const entries = await readIndex(dir)
  let present: string[]
  try {
    present = await readdir(dir)
  } catch {
    return []
  }
  const alive: SavedSkin[] = []
  const survivors: IndexEntry[] = []

  for (const entry of entries) {
    const file = `${entry.id}.png`
    if (!present.includes(file)) {
      continue
    }
    try {
      const bytes = await readFile(join(dir, file))
      alive.push({
        id: entry.id,
        name: entry.name,
        slim: entry.slim,
        dataUrl: `data:image/png;base64,${bytes.toString('base64')}`
      })
      survivors.push(entry)
    } catch {
      // Unreadable file: treat as gone.
    }
  }

  if (survivors.length !== entries.length) {
    await writeIndex(dir, survivors)
  }
  return alive
}

/** Copy a PNG into the library under a unique id. */
export async function saveSkinFile(
  dir: string,
  sourcePath: string,
  name: string,
  slim: boolean
): Promise<SavedSkin[]> {
  await mkdir(dir, { recursive: true })
  const entries = await readIndex(dir)
  const id = uniqueId(toSkinId(name), new Set(entries.map((e) => e.id)))
  await copyFile(sourcePath, join(dir, `${id}.png`))
  entries.push({ id, name: name.trim() || id, slim })
  await writeIndex(dir, entries)
  logLine('skins', `saved "${name}" to the library`)
  return listSavedSkins(dir)
}

/** Save skin bytes already in memory (used for "save my current skin"). */
export async function saveSkinData(
  dir: string,
  dataUrl: string,
  name: string,
  slim: boolean
): Promise<SavedSkin[]> {
  const base64 = dataUrl.replace(/^data:image\/png;base64,/, '')
  if (base64 === dataUrl) {
    throw new Error('That is not a PNG image.')
  }
  await mkdir(dir, { recursive: true })
  const entries = await readIndex(dir)
  const id = uniqueId(toSkinId(name), new Set(entries.map((e) => e.id)))
  await writeFile(join(dir, `${id}.png`), Buffer.from(base64, 'base64'))
  entries.push({ id, name: name.trim() || id, slim })
  await writeIndex(dir, entries)
  logLine('skins', `saved current skin as "${name}"`)
  return listSavedSkins(dir)
}

export async function deleteSavedSkin(dir: string, id: string): Promise<SavedSkin[]> {
  await rm(join(dir, `${id}.png`), { force: true })
  const entries = await readIndex(dir)
  await writeIndex(
    dir,
    entries.filter((e) => e.id !== id)
  )
  return listSavedSkins(dir)
}

/** Absolute path of a saved skin's PNG, for handing to the upload API. */
export function savedSkinPath(dir: string, id: string): string {
  return join(dir, `${id}.png`)
}
