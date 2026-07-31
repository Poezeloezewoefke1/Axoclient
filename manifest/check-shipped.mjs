#!/usr/bin/env node
/**
 * Guard against the failure that shipped a broken client for eleven days:
 * client/ kept gaining fixes and modules, but no release was cut and the
 * manifest kept naming axoclient-0.1.0.jar. CI was green the whole time,
 * because nothing compared what we BUILD against what we SHIP.
 *
 * This fails when client/gradle.properties `mod_version` and the version
 * every manifest channel points at drift apart. Red here means one of:
 *   - you bumped mod_version but never cut the release  -> run the
 *     "Client Release" workflow for tag client-v<version>
 *   - you cut the release but never updated the manifest -> put the new
 *     url/sha1/size in manifest/axo-manifest.json (and the launcher's
 *     bundled fallbackManifest.ts, which fallbackManifest.test.ts keeps
 *     in lockstep)
 *
 * Usage: node manifest/check-shipped.mjs [manifest.json]
 */
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const manifestPath = process.argv[2] ?? join(root, 'manifest', 'axo-manifest.json')

const props = readFileSync(join(root, 'client', 'gradle.properties'), 'utf8')
const built = props.match(/^\s*mod_version\s*=\s*(.+?)\s*$/m)?.[1]
if (!built) {
  console.error('check-shipped: no mod_version in client/gradle.properties')
  process.exit(1)
}

const manifest = JSON.parse(readFileSync(manifestPath, 'utf8'))
const problems = []

for (const [channel, data] of Object.entries(manifest.channels ?? {})) {
  for (const version of data.versions ?? []) {
    const client = version.client ?? {}
    if (client.version !== built) {
      problems.push(
        `${channel}/${version.id}: manifest ships client ${client.version}, but client/gradle.properties builds ${built}`
      )
      continue
    }
    // A matching version string is not enough — the URL has to name the
    // same jar, or we ship an old artifact under a new version number.
    if (!client.url?.includes(`axoclient-${built}.jar`)) {
      problems.push(`${channel}/${version.id}: client.url does not point at axoclient-${built}.jar (${client.url})`)
    }
    if (!client.url?.includes(`client-v${built}/`)) {
      problems.push(`${channel}/${version.id}: client.url is not from the client-v${built} release (${client.url})`)
    }
  }
}

if (problems.length > 0) {
  console.error(`check-shipped: built client is ${built}, but the manifest disagrees:\n`)
  for (const p of problems) console.error(`  - ${p}`)
  console.error('\nSee the header of manifest/check-shipped.mjs for how to fix this.')
  process.exit(1)
}

console.log(`check-shipped: OK — client ${built} is what the manifest ships in every channel`)
