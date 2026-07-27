import { readdir, readFile, stat } from 'node:fs/promises'
import { join } from 'node:path'
import { instanceDir } from './paths'
import type { CrashDiagnosis } from '../shared/types'

/**
 * Turns a Minecraft crash report into something a player can act on.
 *
 * The parsing is deliberately pure (`diagnoseCrash`) so it can be unit
 * tested against real reports; only `findLatestCrash` touches disk. Patterns
 * are ordered most-specific first — an Axo module named in the stack is a far
 * better answer than "a mod crashed".
 */

/** "FullbrightModule" -> "Fullbright"; "CpsCounterModule" -> "Cps Counter". */
function friendlyModuleName(className: string): string {
  return className
    .replace(/Module$/, '')
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .trim()
}

function firstMatch(text: string, pattern: RegExp): string | undefined {
  const match = text.match(pattern)
  return match?.[1]?.trim()
}

/**
 * Inspect a crash report's text. Always returns a diagnosis — an unknown
 * crash still gets the report's own description rather than a shrug.
 */
export function diagnoseCrash(report: string): CrashDiagnosis {
  const description = firstMatch(report, /^Description:\s*(.+)$/m)
  const causedBy = firstMatch(report, /^Caused by:\s*(.+)$/m)
  const topError = firstMatch(report, /^([a-z][\w.]*(?:Exception|Error)(?:.*)?)$/m)
  const technical = causedBy ?? topError ?? description

  // An Axo module in the stack is the most useful answer we can give.
  const axoModule = firstMatch(report, /dev\.axoclient\.modules\.[\w.]*?\.(\w+Module)\./)
  if (axoModule) {
    const name = friendlyModuleName(axoModule)
    return {
      summary: `The "${name}" feature caused the crash.`,
      advice: `Turn ${name} off in the in-game menu (Right Shift), then play again. If you can't get in-game, use Repair below.`,
      culprit: name,
      technical
    }
  }

  if (/OutOfMemoryError|GC overhead limit/i.test(report)) {
    return {
      summary: 'Minecraft ran out of memory.',
      advice: 'Open Settings and give the game more memory, then try again.',
      technical
    }
  }

  if (/UnsupportedClassVersionError|has been compiled by a more recent version/i.test(report)) {
    return {
      summary: 'The game ran on the wrong version of Java.',
      advice: 'Press Repair — the launcher will reinstall the correct Java.',
      technical
    }
  }

  if (/Pixel format not accelerated|Failed to create window|GLFW error|EXT_framebuffer_object|OpenGL 3\.2|No OpenGL context/i.test(report)) {
    return {
      summary: "Your graphics driver couldn't start the game.",
      advice: 'Update your graphics drivers, then try again. Restarting your PC after updating helps.',
      technical
    }
  }

  if (/Mixin (apply|prepare|transformation) failed|InvalidInjectionException|mixin\.injection\.throwables/i.test(report)) {
    return {
      summary: "A mod doesn't match this Minecraft version.",
      advice: 'Press Repair to reinstall the correct mods for this version.',
      technical
    }
  }

  if (/Incompatible mod set|requires (any )?version|Duplicate mod|missing dependenc/i.test(report)) {
    return {
      summary: "The installed mods don't fit together.",
      advice: 'Press Repair to restore the correct mod set.',
      technical
    }
  }

  if (/java\.io\.IOException|No space left on device|Access is denied|FileSystemException/i.test(report)) {
    return {
      summary: 'The game could not read or write its files.',
      advice: 'Check you have free disk space, then press Repair.',
      technical
    }
  }

  return {
    summary: description ? `Minecraft crashed while: ${description}` : 'Minecraft crashed.',
    advice: 'Press Repair to re-check your files. If it keeps happening, open the logs and share them.',
    technical
  }
}

/**
 * Newest crash report for a version, already diagnosed. Returns null when the
 * game has never written one (or the folder is unreadable) — a missing report
 * is normal, not an error.
 */
export async function findLatestCrash(
  installDir: string,
  versionId: string,
  /** Ignore reports older than this, so we don't resurface last week's crash. */
  notBefore?: number
): Promise<CrashDiagnosis | null> {
  const dir = join(instanceDir(installDir, versionId), 'crash-reports')
  try {
    const names = (await readdir(dir)).filter((n) => n.endsWith('.txt'))
    if (names.length === 0) {
      return null
    }
    const stamped = await Promise.all(
      names.map(async (name) => {
        const path = join(dir, name)
        return { path, mtime: (await stat(path)).mtimeMs }
      })
    )
    stamped.sort((a, b) => b.mtime - a.mtime)
    const newest = stamped[0]
    if (notBefore !== undefined && newest.mtime < notBefore) {
      return null
    }
    return diagnoseCrash(await readFile(newest.path, 'utf8'))
  } catch {
    return null
  }
}
