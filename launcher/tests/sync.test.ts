import { createHash } from 'node:crypto'
import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { createServer, type Server } from 'node:http'
import type { AddressInfo } from 'node:net'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it } from 'vitest'
import { downloadFile } from '../src/main/download'
import { syncDirectory, type DesiredFile } from '../src/main/sync'

/** Local fixture server — exercises the real fetch path without external network. */
const FILES: Record<string, Buffer> = {
  '/mod-a.jar': Buffer.from('mod-a contents'),
  '/mod-b.jar': Buffer.from('mod-b contents')
}
let flakyRemaining = 0
let server: Server
let baseUrl: string

const sha1 = (data: Buffer): string => createHash('sha1').update(data).digest('hex')

beforeAll(async () => {
  server = createServer((req, res) => {
    if (req.url === '/flaky.jar') {
      // Serves garbage until flakyRemaining hits 0, then the real bytes.
      const body = flakyRemaining-- > 0 ? Buffer.from('corrupted!') : FILES['/mod-a.jar']
      res.writeHead(200).end(body)
      return
    }
    const body = req.url ? FILES[req.url] : undefined
    if (body) {
      res.writeHead(200).end(body)
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

describe('downloadFile', () => {
  let dir: string

  beforeEach(async () => {
    dir = await mkdtemp(join(tmpdir(), 'axo-dl-'))
  })

  afterEach(async () => {
    await rm(dir, { recursive: true, force: true })
  })

  it('downloads and verifies a file', async () => {
    const dest = join(dir, 'a.jar')
    await downloadFile(`${baseUrl}/mod-a.jar`, dest, sha1(FILES['/mod-a.jar']))
    expect(await readFile(dest)).toEqual(FILES['/mod-a.jar'])
  })

  it('retries once after a hash mismatch and succeeds', async () => {
    flakyRemaining = 1
    const dest = join(dir, 'flaky.jar')
    await downloadFile(`${baseUrl}/flaky.jar`, dest, sha1(FILES['/mod-a.jar']))
    expect(await readFile(dest)).toEqual(FILES['/mod-a.jar'])
  })

  it('throws after exhausting attempts on persistent mismatch', async () => {
    flakyRemaining = 99
    const dest = join(dir, 'bad.jar')
    await expect(
      downloadFile(`${baseUrl}/flaky.jar`, dest, sha1(FILES['/mod-a.jar']))
    ).rejects.toThrow(/sha1 mismatch/)
  })

  it('throws on HTTP errors', async () => {
    await expect(downloadFile(`${baseUrl}/missing.jar`, join(dir, 'x.jar'))).rejects.toThrow(
      /HTTP 404/
    )
  })
})

describe('syncDirectory', () => {
  let dir: string
  let desired: DesiredFile[]

  beforeEach(async () => {
    dir = await mkdtemp(join(tmpdir(), 'axo-sync-'))
    desired = [
      { fileName: 'mod-a.jar', url: `${baseUrl}/mod-a.jar`, sha1: sha1(FILES['/mod-a.jar']) },
      { fileName: 'mod-b.jar', url: `${baseUrl}/mod-b.jar`, sha1: sha1(FILES['/mod-b.jar']) }
    ]
  })

  afterEach(async () => {
    await rm(dir, { recursive: true, force: true })
  })

  it('downloads everything into an empty directory', async () => {
    const result = await syncDirectory(dir, desired)
    expect(result.downloaded.sort()).toEqual(['mod-a.jar', 'mod-b.jar'])
    expect(result.kept).toEqual([])
    expect(result.removed).toEqual([])
  })

  it('is idempotent on a correct directory', async () => {
    await syncDirectory(dir, desired)
    const second = await syncDirectory(dir, desired)
    expect(second.downloaded).toEqual([])
    expect(second.kept.sort()).toEqual(['mod-a.jar', 'mod-b.jar'])
  })

  it('re-downloads corrupted files and removes stray jars, keeping other files', async () => {
    await syncDirectory(dir, desired)
    await writeFile(join(dir, 'mod-a.jar'), 'tampered')
    await writeFile(join(dir, 'stray.jar'), 'not wanted')
    await writeFile(join(dir, 'notes.txt'), 'user file — must survive')

    const result = await syncDirectory(dir, desired)
    expect(result.downloaded).toEqual(['mod-a.jar'])
    expect(result.removed).toEqual(['stray.jar'])
    expect(await readFile(join(dir, 'mod-a.jar'))).toEqual(FILES['/mod-a.jar'])
    expect(await readFile(join(dir, 'notes.txt'), 'utf8')).toBe('user file — must survive')
  })
})
