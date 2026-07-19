import { createHash } from 'node:crypto'
import { mkdtemp, readFile, rm } from 'node:fs/promises'
import { createServer, type Server } from 'node:http'
import type { AddressInfo } from 'node:net'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it } from 'vitest'
import { adoptiumAssetsUrl, ensureJava, pickBinary } from '../src/main/java'

const FIXTURE_ZIP = join(__dirname, 'fixtures', 'fake-jre.zip')

let zipBytes: Buffer
let zipSha256: string
let server: Server
let baseUrl: string

beforeAll(async () => {
  zipBytes = await readFile(FIXTURE_ZIP)
  zipSha256 = createHash('sha256').update(zipBytes).digest('hex')
  server = createServer((req, res) => {
    if (req.url === '/jre.zip') {
      res.writeHead(200).end(zipBytes)
    } else {
      res.writeHead(404).end()
    }
  })
  await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve))
  baseUrl = `http://127.0.0.1:${(server.address() as AddressInfo).port}`
})

afterAll(() => {
  server.close()
})

describe('adoptiumAssetsUrl', () => {
  it('builds the Adoptium latest-assets URL', () => {
    expect(adoptiumAssetsUrl(21)).toBe(
      'https://api.adoptium.net/v3/assets/latest/21/hotspot?os=windows&architecture=x64&image_type=jre&vendor=eclipse'
    )
  })
})

describe('pickBinary', () => {
  it('extracts url, sha256, and release name from the API response', () => {
    const picked = pickBinary([
      {
        release_name: 'jdk-21.0.5+11',
        binary: { package: { link: 'https://example.com/a.zip', checksum: 'a'.repeat(64) } }
      }
    ])
    expect(picked).toEqual({
      url: 'https://example.com/a.zip',
      sha256: 'a'.repeat(64),
      releaseName: 'jdk-21.0.5+11'
    })
  })

  it('rejects an empty or malformed response', () => {
    expect(() => pickBinary([])).toThrow()
    expect(() => pickBinary({ nope: true })).toThrow()
  })
})

describe('ensureJava', () => {
  let runtimeRoot: string

  const apiResponse = (): unknown => [
    {
      release_name: 'fake-jre-21.0.1+9',
      binary: { package: { link: `${baseUrl}/jre.zip`, checksum: zipSha256 } }
    }
  ]

  beforeEach(async () => {
    runtimeRoot = await mkdtemp(join(tmpdir(), 'axo-jre-'))
  })

  afterEach(async () => {
    await rm(runtimeRoot, { recursive: true, force: true })
  })

  it('provisions a JRE end-to-end and finds the executable', async () => {
    const stages: string[] = []
    const javaPath = await ensureJava(21, runtimeRoot, {
      fetchJson: async () => apiResponse(),
      exeRelPath: join('bin', 'java.txt'),
      onProgress: (stage) => stages.push(stage)
    })
    expect(javaPath).toBe(join(runtimeRoot, '21', 'fake-jre-21.0.1+9', 'bin', 'java.txt'))
    expect(await readFile(javaPath, 'utf8')).toBe('fake java executable')
    expect(stages).toEqual(['query', 'download', 'extract'])
  })

  it('uses the cache on the second call without touching the network', async () => {
    await ensureJava(21, runtimeRoot, {
      fetchJson: async () => apiResponse(),
      exeRelPath: join('bin', 'java.txt')
    })
    const javaPath = await ensureJava(21, runtimeRoot, {
      fetchJson: async () => {
        throw new Error('network must not be used on cache hit')
      },
      exeRelPath: join('bin', 'java.txt')
    })
    expect(javaPath).toContain('java.txt')
  })

  it('re-provisions when the cached executable disappeared', async () => {
    const first = await ensureJava(21, runtimeRoot, {
      fetchJson: async () => apiResponse(),
      exeRelPath: join('bin', 'java.txt')
    })
    await rm(first)
    const second = await ensureJava(21, runtimeRoot, {
      fetchJson: async () => apiResponse(),
      exeRelPath: join('bin', 'java.txt')
    })
    expect(await readFile(second, 'utf8')).toBe('fake java executable')
  })

  it('fails on a sha256 mismatch instead of extracting a tampered archive', async () => {
    await expect(
      ensureJava(21, runtimeRoot, {
        fetchJson: async () => [
          {
            release_name: 'fake-jre-21.0.1+9',
            binary: { package: { link: `${baseUrl}/jre.zip`, checksum: 'f'.repeat(64) } }
          }
        ],
        exeRelPath: join('bin', 'java.txt')
      })
    ).rejects.toThrow(/sha256 mismatch/)
  })
})
