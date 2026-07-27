import { copyFile, mkdir, readdir, readFile, rename, rm, stat, writeFile } from 'node:fs/promises'
import { basename, join } from 'node:path'
import { instanceDir, instanceModsDir } from './paths'
import { logLine } from './logger'
import type { UserMod } from '../shared/types'

/**
 * Mods the player adds themselves, per version.
 *
 * The mods folder is launcher-owned and stray .jar files are deleted on every
 * sync, so user mods must be recorded to survive: this registry is what tells
 * the sync "keep that one". Disabling renames the jar to `.jar.disabled`,
 * which both Fabric and the sync ignore, so a disabled mod stays on disk
 * without loading.
 */

const REGISTRY_FILE = 'axo-usermods.json'
const DISABLED_SUFFIX = '.disabled'

interface Registry {
  mods: { fileName: string; enabled: boolean }[]
}

function registryPath(installDir: string, versionId: string): string {
  return join(instanceDir(installDir, versionId), REGISTRY_FILE)
}

async function readRegistry(installDir: string, versionId: string): Promise<Registry> {
  try {
    const parsed = JSON.parse(
      await readFile(registryPath(installDir, versionId), 'utf8')
    ) as Partial<Registry>
    if (!parsed || !Array.isArray(parsed.mods)) {
      return { mods: [] }
    }
    return {
      mods: parsed.mods.filter(
        (m): m is { fileName: string; enabled: boolean } =>
          typeof m?.fileName === 'string' && m.fileName.endsWith('.jar')
      )
    }
  } catch {
    return { mods: [] }
  }
}

async function writeRegistry(
  installDir: string,
  versionId: string,
  registry: Registry
): Promise<void> {
  const dir = instanceDir(installDir, versionId)
  await mkdir(dir, { recursive: true })
  await writeFile(registryPath(installDir, versionId), JSON.stringify(registry, null, 2), 'utf8')
}

async function exists(path: string): Promise<boolean> {
  try {
    await stat(path)
    return true
  } catch {
    return false
  }
}

/**
 * The player's mods for a version, dropping registry entries whose file has
 * been deleted outside the launcher so the list never lies.
 */
export async function listUserMods(installDir: string, versionId: string): Promise<UserMod[]> {
  const registry = await readRegistry(installDir, versionId)
  const modsDir = instanceModsDir(installDir, versionId)
  const alive: UserMod[] = []
  let pruned = false

  for (const entry of registry.mods) {
    const enabledPath = join(modsDir, entry.fileName)
    const disabledPath = `${enabledPath}${DISABLED_SUFFIX}`
    const enabledExists = await exists(enabledPath)
    const disabledExists = enabledExists ? false : await exists(disabledPath)
    if (!enabledExists && !disabledExists) {
      pruned = true
      continue
    }
    alive.push({ fileName: entry.fileName, enabled: enabledExists })
  }

  if (pruned) {
    await writeRegistry(installDir, versionId, {
      mods: alive.map((m) => ({ fileName: m.fileName, enabled: m.enabled }))
    })
  }
  return alive
}

/** Filenames that stray-removal must not delete (enabled user mods). */
export async function protectedModFiles(
  installDir: string,
  versionId: string
): Promise<Set<string>> {
  const mods = await listUserMods(installDir, versionId)
  return new Set(mods.filter((m) => m.enabled).map((m) => m.fileName))
}

/**
 * Copy jars into the version's mods folder and record them. Returns the
 * resulting list. Names that collide with an existing mod are suffixed so a
 * second "sodium.jar" can't silently replace the first.
 */
export async function addUserMods(
  installDir: string,
  versionId: string,
  sourcePaths: string[]
): Promise<UserMod[]> {
  const modsDir = instanceModsDir(installDir, versionId)
  await mkdir(modsDir, { recursive: true })
  const registry = await readRegistry(installDir, versionId)
  const taken = new Set(registry.mods.map((m) => m.fileName))

  for (const source of sourcePaths) {
    if (!source.toLowerCase().endsWith('.jar')) {
      continue
    }
    let fileName = basename(source)
    let counter = 2
    while (taken.has(fileName) || (await exists(join(modsDir, fileName)))) {
      fileName = `${basename(source, '.jar')}-${counter}.jar`
      counter += 1
    }
    await copyFile(source, join(modsDir, fileName))
    registry.mods.push({ fileName, enabled: true })
    taken.add(fileName)
    logLine('usermods', `added ${fileName} to ${versionId}`)
  }

  await writeRegistry(installDir, versionId, registry)
  return listUserMods(installDir, versionId)
}

/** Turn a mod on or off by renaming it; Fabric only loads plain .jar files. */
export async function setUserModEnabled(
  installDir: string,
  versionId: string,
  fileName: string,
  enabled: boolean
): Promise<UserMod[]> {
  const modsDir = instanceModsDir(installDir, versionId)
  const enabledPath = join(modsDir, fileName)
  const disabledPath = `${enabledPath}${DISABLED_SUFFIX}`
  try {
    if (enabled && (await exists(disabledPath))) {
      await rename(disabledPath, enabledPath)
    } else if (!enabled && (await exists(enabledPath))) {
      await rename(enabledPath, disabledPath)
    }
    const registry = await readRegistry(installDir, versionId)
    for (const mod of registry.mods) {
      if (mod.fileName === fileName) {
        mod.enabled = enabled
      }
    }
    await writeRegistry(installDir, versionId, registry)
  } catch (error) {
    logLine('usermods', `toggle failed for ${fileName}: ${error instanceof Error ? error.message : error}`)
  }
  return listUserMods(installDir, versionId)
}

export async function removeUserMod(
  installDir: string,
  versionId: string,
  fileName: string
): Promise<UserMod[]> {
  const modsDir = instanceModsDir(installDir, versionId)
  await rm(join(modsDir, fileName), { force: true })
  await rm(join(modsDir, `${fileName}${DISABLED_SUFFIX}`), { force: true })
  const registry = await readRegistry(installDir, versionId)
  await writeRegistry(installDir, versionId, {
    mods: registry.mods.filter((m) => m.fileName !== fileName)
  })
  logLine('usermods', `removed ${fileName} from ${versionId}`)
  return listUserMods(installDir, versionId)
}

/** Disabled jars left behind by an older launcher build, for diagnostics. */
export async function orphanedDisabledFiles(
  installDir: string,
  versionId: string
): Promise<string[]> {
  try {
    const names = await readdir(instanceModsDir(installDir, versionId))
    const registry = await readRegistry(installDir, versionId)
    const known = new Set(registry.mods.map((m) => `${m.fileName}${DISABLED_SUFFIX}`))
    return names.filter((n) => n.endsWith(DISABLED_SUFFIX) && !known.has(n))
  } catch {
    return []
  }
}
