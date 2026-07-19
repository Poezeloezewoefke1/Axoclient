#!/usr/bin/env node
/**
 * Validates axo-manifest.json against docs/manifest-spec.md (P3-08).
 *
 * Dependency-free on purpose: CI and contributors can run it with nothing but
 * Node >= 18 (`node manifest/validate.mjs`). The launcher keeps its own zod
 * schema (launcher/src/main/manifest.ts) — if you change the spec, change both
 * in the same PR.
 *
 * Usage:
 *   node manifest/validate.mjs [file] [--allow-placeholders]
 *
 * Placeholder sha1s (all zeros) and PLACEHOLDER tokens are errors unless
 * --allow-placeholders is passed (pre-first-release mode, see P4-07).
 */
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

const args = process.argv.slice(2)
const allowPlaceholders = args.includes('--allow-placeholders')
const file =
  args.find((a) => !a.startsWith('--')) ??
  join(dirname(fileURLToPath(import.meta.url)), 'axo-manifest.json')

const errors = []
const warnings = []

const SHA1_RE = /^[0-9a-f]{40}$/
const PLACEHOLDER_SHA1 = '0'.repeat(40)
const SEMVER_RE = /^\d+\.\d+\.\d+$/

function fail(path, message) {
  errors.push(`${path}: ${message}`)
}

function placeholder(path, message) {
  if (allowPlaceholders) {
    warnings.push(`${path}: ${message}`)
  } else {
    errors.push(`${path}: ${message} (run with --allow-placeholders before the first release)`)
  }
}

function isString(v) {
  return typeof v === 'string' && v.length > 0
}

function checkArtifact(a, path, { isMod = false } = {}) {
  if (typeof a !== 'object' || a === null) {
    return fail(path, 'must be an object')
  }
  if (!isString(a.version)) fail(`${path}.version`, 'required non-empty string')
  if (!isString(a.url) || !a.url.startsWith('https://')) {
    fail(`${path}.url`, 'required https:// URL')
  }
  if (!isString(a.sha1) || !SHA1_RE.test(a.sha1)) {
    fail(`${path}.sha1`, 'required 40-char lowercase hex sha1')
  } else if (a.sha1 === PLACEHOLDER_SHA1) {
    placeholder(`${path}.sha1`, 'placeholder (all-zero) sha1')
  }
  if (a.size !== undefined && !Number.isInteger(a.size)) {
    fail(`${path}.size`, 'must be an integer when present')
  }
  for (const field of ['version', 'url']) {
    if (isString(a[field]) && a[field].includes('PLACEHOLDER')) {
      placeholder(`${path}.${field}`, 'contains PLACEHOLDER token')
    }
  }
  if (isMod) {
    if (!isString(a.id)) fail(`${path}.id`, 'required non-empty string')
    if (a.source !== 'modrinth') fail(`${path}.source`, 'must be "modrinth" in schema v1')
    if (!isString(a.modrinthProject)) fail(`${path}.modrinthProject`, 'required non-empty string')
    if (!isString(a.modrinthVersion)) {
      fail(`${path}.modrinthVersion`, 'required non-empty string')
    } else if (a.modrinthVersion.includes('PLACEHOLDER')) {
      placeholder(`${path}.modrinthVersion`, 'contains PLACEHOLDER token')
    }
    if (a.required !== undefined && typeof a.required !== 'boolean') {
      fail(`${path}.required`, 'must be a boolean when present')
    }
  }
}

// ---- load ----
let manifest
try {
  manifest = JSON.parse(readFileSync(file, 'utf8'))
} catch (e) {
  console.error(`✖ ${file}: ${e.message}`)
  process.exit(1)
}

// ---- top level ----
if (manifest.schemaVersion !== 1) fail('schemaVersion', 'must be the integer 1')
if (!isString(manifest.generatedAt) || Number.isNaN(Date.parse(manifest.generatedAt))) {
  fail('generatedAt', 'required ISO-8601 timestamp')
}
if (typeof manifest.launcher !== 'object' || manifest.launcher === null) {
  fail('launcher', 'required object')
} else {
  if (!isString(manifest.launcher.minimumVersion) || !SEMVER_RE.test(manifest.launcher.minimumVersion)) {
    fail('launcher.minimumVersion', 'required semver string (X.Y.Z)')
  }
  if (!isString(manifest.launcher.releasesRepo) || !/^[\w.-]+\/[\w.-]+$/.test(manifest.launcher.releasesRepo)) {
    fail('launcher.releasesRepo', 'required "owner/repo" string')
  }
}

// ---- channels ----
const seenVersionIds = new Set()
if (typeof manifest.channels !== 'object' || manifest.channels === null || Object.keys(manifest.channels).length === 0) {
  fail('channels', 'required non-empty object')
} else {
  for (const [name, channel] of Object.entries(manifest.channels)) {
    const cpath = `channels.${name}`
    if (typeof channel !== 'object' || channel === null) {
      fail(cpath, 'must be an object')
      continue
    }
    if (!Array.isArray(channel.versions)) {
      fail(`${cpath}.versions`, 'required array')
      continue
    }
    if (!isString(channel.default)) {
      fail(`${cpath}.default`, 'required non-empty string')
    } else if (!channel.versions.some((v) => v && v.id === channel.default)) {
      fail(`${cpath}.default`, `"${channel.default}" not found in ${cpath}.versions`)
    }
    channel.versions.forEach((version, i) => {
      const vpath = `${cpath}.versions[${i}]`
      if (typeof version !== 'object' || version === null) {
        return fail(vpath, 'must be an object')
      }
      if (!isString(version.id)) {
        fail(`${vpath}.id`, 'required non-empty string')
      } else if (seenVersionIds.has(version.id)) {
        fail(`${vpath}.id`, `duplicate version id "${version.id}" (must be unique across channels)`)
      } else {
        seenVersionIds.add(version.id)
      }
      if (!isString(version.mcVersion)) fail(`${vpath}.mcVersion`, 'required non-empty string')
      if (!isString(version.fabricLoaderVersion)) {
        fail(`${vpath}.fabricLoaderVersion`, 'required non-empty string')
      }
      if (!Number.isInteger(version.javaMajor) || version.javaMajor < 8) {
        fail(`${vpath}.javaMajor`, 'required integer >= 8')
      }
      if (version.notes !== undefined && !isString(version.notes)) {
        fail(`${vpath}.notes`, 'must be a non-empty string when present')
      }
      checkArtifact(version.client, `${vpath}.client`)
      if (!Array.isArray(version.mods)) {
        fail(`${vpath}.mods`, 'required array')
      } else {
        version.mods.forEach((mod, j) => checkArtifact(mod, `${vpath}.mods[${j}]`, { isMod: true }))
      }
    })
  }
}

// ---- report ----
for (const w of warnings) console.warn(`⚠ ${w}`)
for (const e of errors) console.error(`✖ ${e}`)
if (errors.length > 0) {
  console.error(`\n${file}: INVALID (${errors.length} error(s), ${warnings.length} warning(s))`)
  process.exit(1)
}
console.log(`${file}: OK (${warnings.length} placeholder warning(s))`)
