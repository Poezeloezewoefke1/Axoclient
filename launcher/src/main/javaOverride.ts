import { stat } from 'node:fs/promises'
import { basename } from 'node:path'

/**
 * Validation for the "use my own Java" setting.
 *
 * The managed Adoptium runtime is right for almost everyone, so this exists
 * for the cases it can't cover: a machine that already has a tuned JDK, an
 * architecture Adoptium doesn't publish for, or a corporate image where
 * downloading a runtime is blocked.
 *
 * Everything here is pure except {@link validateJavaPath}, which only stats a
 * file — no electron import, so it unit-tests directly.
 */

export interface JavaPathProblem {
  ok: false
  reason: string
}

export interface JavaPathOk {
  ok: true
  path: string
}

export type JavaPathCheck = JavaPathOk | JavaPathProblem

/** Executable names we accept, in order of preference on Windows. */
const WINDOWS_NAMES = ['javaw.exe', 'java.exe']
const UNIX_NAMES = ['java']

/**
 * Is this filename plausibly a Java launcher?
 *
 * Pointing the setting at a folder, or at `javac`, is the common mistake —
 * both would otherwise fail much later with an unhelpful spawn error.
 */
export function looksLikeJavaExecutable(path: string, platform = process.platform): boolean {
  const name = basename(path).toLowerCase()
  return (platform === 'win32' ? WINDOWS_NAMES : UNIX_NAMES).includes(name)
}

/** Human-readable reason a path is unusable, or null when the shape is fine. */
export function describePathProblem(path: string, platform = process.platform): string | null {
  const trimmed = path.trim()
  if (trimmed.length === 0) {
    return 'No path given.'
  }
  const name = basename(trimmed).toLowerCase()
  if (name === 'javac' || name === 'javac.exe') {
    return 'That is the Java compiler. Pick javaw.exe or java.exe instead.'
  }
  if (!looksLikeJavaExecutable(trimmed, platform)) {
    const wanted = platform === 'win32' ? 'javaw.exe or java.exe' : 'java'
    return `Pick the ${wanted} program itself, not a folder.`
  }
  return null
}

/**
 * Full check: shape first (cheap, precise message), then existence.
 *
 * Deliberately does not run `java -version`. Spawning an unknown binary to
 * validate it is a worse trade than letting the launch fail with the real
 * error — and the crash helper already explains Java failures in plain
 * language.
 */
export async function validateJavaPath(
  path: string,
  platform = process.platform
): Promise<JavaPathCheck> {
  const trimmed = path.trim()
  const shapeProblem = describePathProblem(trimmed, platform)
  if (shapeProblem) {
    return { ok: false, reason: shapeProblem }
  }
  try {
    const info = await stat(trimmed)
    if (!info.isFile()) {
      return { ok: false, reason: 'That path is not a file.' }
    }
  } catch {
    return { ok: false, reason: 'That file does not exist.' }
  }
  return { ok: true, path: trimmed }
}

/**
 * The java to launch with: the override when set, otherwise null meaning
 * "provision and use the managed runtime".
 *
 * An override that has gone missing (uninstalled JDK, unplugged drive) falls
 * back to the managed runtime rather than failing the launch — silently
 * working beats a dead Play button.
 */
export async function resolveJavaPath(
  override: string,
  platform = process.platform
): Promise<string | null> {
  if (override.trim().length === 0) {
    return null
  }
  const check = await validateJavaPath(override, platform)
  return check.ok ? check.path : null
}
