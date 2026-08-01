/**
 * Last-resort manifest baked into the launcher build. Used only when BOTH
 * the remote fetch and the on-disk cache fail — e.g. a first run while the
 * GitHub repo is still private, or fully offline.
 *
 * A full mirror of manifest/axo-manifest.json (the release pipeline is the
 * source of truth), enforced by tests/fallbackManifest.test.ts so it cannot
 * silently rot. A stale fallback still lets the launcher open and show a
 * version, and the next successful online fetch overwrites it.
 */
export const FALLBACK_MANIFEST: unknown = {
  schemaVersion: 1,
  generatedAt: '2026-07-20T12:50:00Z',
  launcher: {
    minimumVersion: '0.1.0',
    releasesRepo: 'Poezeloezewoefke1/Axoclient'
  },
  channels: {
    stable: {
      default: '1.21.11-r1',
      versions: [
        {
          id: '1.21.11-r1',
          mcVersion: '1.21.11',
          fabricLoaderVersion: '0.17.3',
          javaMajor: 21,
          notes:
            'Axo Client 0.3.0 for Minecraft 1.21.11 — rebuilt mod menu, smooth zoom, cape and trail pickers, clear liquids and item tooltips.',
          client: {
            version: '0.3.0',
            url: 'https://github.com/Poezeloezewoefke1/Axoclient/releases/download/client-v0.3.0/axoclient-0.3.0.jar',
            sha1: '526fd457f23a91497041212ae72c92e8b8353ea6',
            size: 842169
          },
          mods: [
            {
              id: 'fabric-api',
              source: 'modrinth',
              modrinthProject: 'P7dR8mSH',
              modrinthVersion: 'zGF3drOQ',
              version: '0.141.5+1.21.11',
              url: 'https://cdn.modrinth.com/data/P7dR8mSH/versions/zGF3drOQ/fabric-api-0.141.5%2B1.21.11.jar',
              sha1: 'f56956fc14c6e1af380e80fce7e9cfd4db050eef',
              size: 2425434,
              required: true
            },
            {
              id: 'sodium',
              source: 'modrinth',
              modrinthProject: 'AANobbMI',
              modrinthVersion: 'Ny3XyYle',
              version: '0.8.13+mc1.21.11',
              url: 'https://cdn.modrinth.com/data/AANobbMI/versions/Ny3XyYle/sodium-fabric-0.8.13%2Bmc1.21.11.jar',
              sha1: 'e757883c4959582bed8b057c1f830cb84f325567',
              size: 1907864,
              required: true
            },
            {
              id: 'lithium',
              source: 'modrinth',
              modrinthProject: 'gvQqBUqZ',
              modrinthVersion: 'Ow7wA0kG',
              version: '0.21.4+mc1.21.11',
              url: 'https://cdn.modrinth.com/data/gvQqBUqZ/versions/Ow7wA0kG/lithium-fabric-0.21.4%2Bmc1.21.11.jar',
              sha1: '203bdcb26e97b3217b045e1182651a7d7b6462ec',
              size: 900462,
              required: true
            }
          ]
        }
      ]
    },
    beta: {
      default: '1.21.11-b1',
      versions: [
        {
          id: '1.21.11-b1',
          mcVersion: '1.21.11',
          fabricLoaderVersion: '0.17.3',
          javaMajor: 21,
          notes:
            'Beta channel. Currently the same build as stable \u2014 new versions land here first for testing, then the stable default moves.',
          client: {
            version: '0.3.0',
            url: 'https://github.com/Poezeloezewoefke1/Axoclient/releases/download/client-v0.3.0/axoclient-0.3.0.jar',
            sha1: '526fd457f23a91497041212ae72c92e8b8353ea6',
            size: 842169
          },
          mods: [
            {
              id: 'fabric-api',
              source: 'modrinth',
              modrinthProject: 'P7dR8mSH',
              modrinthVersion: 'zGF3drOQ',
              version: '0.141.5+1.21.11',
              url: 'https://cdn.modrinth.com/data/P7dR8mSH/versions/zGF3drOQ/fabric-api-0.141.5%2B1.21.11.jar',
              sha1: 'f56956fc14c6e1af380e80fce7e9cfd4db050eef',
              size: 2425434,
              required: true
            },
            {
              id: 'sodium',
              source: 'modrinth',
              modrinthProject: 'AANobbMI',
              modrinthVersion: 'Ny3XyYle',
              version: '0.8.13+mc1.21.11',
              url: 'https://cdn.modrinth.com/data/AANobbMI/versions/Ny3XyYle/sodium-fabric-0.8.13%2Bmc1.21.11.jar',
              sha1: 'e757883c4959582bed8b057c1f830cb84f325567',
              size: 1907864,
              required: true
            },
            {
              id: 'lithium',
              source: 'modrinth',
              modrinthProject: 'gvQqBUqZ',
              modrinthVersion: 'Ow7wA0kG',
              version: '0.21.4+mc1.21.11',
              url: 'https://cdn.modrinth.com/data/gvQqBUqZ/versions/Ow7wA0kG/lithium-fabric-0.21.4%2Bmc1.21.11.jar',
              sha1: '203bdcb26e97b3217b045e1182651a7d7b6462ec',
              size: 900462,
              required: true
            }
          ]
        }
      ]
    }
  }
}
