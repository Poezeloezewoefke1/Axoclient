import { readFile } from 'node:fs/promises'
import { join } from 'node:path'
import { describe, expect, it, vi } from 'vitest'

// manifest.ts imports electron for cache paths; stub it so the schema can be
// imported in a plain node test.
vi.mock('electron', () => ({
  app: { getPath: () => '/tmp', getVersion: () => '0.1.0' }
}))

import { manifestSchema } from '../src/main/manifest'
import { FALLBACK_MANIFEST } from '../src/main/fallbackManifest'

// The fallback is the launcher's last line of defence when the remote fetch
// AND the on-disk cache both fail (private repo, offline first run). If it
// ever stops validating, the 404 the user hit comes straight back — so these
// tests guard both its validity and that it advertises the same version data
// as the source-of-truth manifest.
describe('fallback manifest', () => {
  it('validates against the manifest schema', () => {
    const parsed = manifestSchema.safeParse(FALLBACK_MANIFEST)
    expect(parsed.success).toBe(true)
  })

  it('stays in sync with manifest/axo-manifest.json (ignoring timestamp/comments)', async () => {
    const diskRaw = JSON.parse(
      await readFile(join(process.cwd(), '..', 'manifest', 'axo-manifest.json'), 'utf8')
    )
    const disk = manifestSchema.parse(diskRaw)
    const fallback = manifestSchema.parse(FALLBACK_MANIFEST)

    // generatedAt drifts each time the manifest is regenerated; the payload
    // that actually drives installs (launcher gate + channels) must match.
    expect(fallback.schemaVersion).toEqual(disk.schemaVersion)
    expect(fallback.launcher).toEqual(disk.launcher)
    expect(fallback.channels).toEqual(disk.channels)
  })
})
