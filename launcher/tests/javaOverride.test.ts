import { mkdtemp, rm, writeFile, mkdir } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import {
  describePathProblem,
  looksLikeJavaExecutable,
  resolveJavaPath,
  validateJavaPath
} from '../src/main/javaOverride'

describe('looksLikeJavaExecutable', () => {
  it('accepts the Windows launchers', () => {
    expect(looksLikeJavaExecutable('C:/jdk/bin/javaw.exe', 'win32')).toBe(true)
    expect(looksLikeJavaExecutable('C:/jdk/bin/java.exe', 'win32')).toBe(true)
  })

  it('is case-insensitive', () => {
    expect(looksLikeJavaExecutable('C:/jdk/bin/JAVAW.EXE', 'win32')).toBe(true)
  })

  it('accepts bare java elsewhere', () => {
    expect(looksLikeJavaExecutable('/usr/lib/jvm/jdk-21/bin/java', 'linux')).toBe(true)
    expect(looksLikeJavaExecutable('/usr/lib/jvm/jdk-21/bin/java.exe', 'linux')).toBe(false)
  })

  it('rejects a folder', () => {
    expect(looksLikeJavaExecutable('C:/jdk/bin', 'win32')).toBe(false)
  })
})

describe('describePathProblem', () => {
  it('passes a good path', () => {
    expect(describePathProblem('C:/jdk/bin/javaw.exe', 'win32')).toBeNull()
  })

  it('calls out an empty path', () => {
    expect(describePathProblem('   ', 'win32')).toBe('No path given.')
  })

  it('calls out the compiler specifically', () => {
    expect(describePathProblem('C:/jdk/bin/javac.exe', 'win32')).toMatch(/compiler/i)
    expect(describePathProblem('/usr/bin/javac', 'linux')).toMatch(/compiler/i)
  })

  it('names the right executable per platform', () => {
    expect(describePathProblem('C:/jdk/bin', 'win32')).toMatch(/javaw\.exe or java\.exe/)
    expect(describePathProblem('/usr/lib/jvm', 'linux')).toMatch(/Pick the java program/)
  })
})

describe('validateJavaPath / resolveJavaPath', () => {
  let dir = ''
  let exe = ''

  beforeEach(async () => {
    dir = await mkdtemp(join(tmpdir(), 'axo-java-'))
    await mkdir(join(dir, 'bin'), { recursive: true })
    exe = join(dir, 'bin', 'java')
    await writeFile(exe, '#!/bin/sh\n', 'utf8')
  })

  afterEach(async () => {
    await rm(dir, { recursive: true, force: true })
  })

  it('accepts a real file with the right name', async () => {
    const result = await validateJavaPath(exe, 'linux')
    expect(result).toEqual({ ok: true, path: exe })
  })

  it('trims surrounding whitespace', async () => {
    const result = await validateJavaPath(`  ${exe}  `, 'linux')
    expect(result).toEqual({ ok: true, path: exe })
  })

  it('rejects a missing file', async () => {
    const result = await validateJavaPath(join(dir, 'bin', 'nope', 'java'), 'linux')
    expect(result).toEqual({ ok: false, reason: 'That file does not exist.' })
  })

  it('rejects a directory even when it is named java', async () => {
    const asDir = join(dir, 'java')
    await mkdir(asDir)
    const result = await validateJavaPath(asDir, 'linux')
    expect(result).toEqual({ ok: false, reason: 'That path is not a file.' })
  })

  it('reports the shape problem before touching the disk', async () => {
    const result = await validateJavaPath(join(dir, 'bin', 'javac'), 'linux')
    expect(result).toEqual({
      ok: false,
      reason: 'That is the Java compiler. Pick javaw.exe or java.exe instead.'
    })
  })

  it('resolves to null when unset, meaning use the managed runtime', async () => {
    expect(await resolveJavaPath('', 'linux')).toBeNull()
    expect(await resolveJavaPath('   ', 'linux')).toBeNull()
  })

  it('resolves to the override when it is valid', async () => {
    expect(await resolveJavaPath(exe, 'linux')).toBe(exe)
  })

  it('falls back to the managed runtime when the override has gone missing', async () => {
    await rm(exe)
    expect(await resolveJavaPath(exe, 'linux')).toBeNull()
  })
})
