import { createHash } from 'node:crypto'
import { mkdtemp, readFile, rm, stat } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { downloadFile } from '../src/main/download'

const sha1 = (b: Buffer): string => createHash('sha1').update(b).digest('hex')

function bufferedResponse(body: Buffer, total: number | undefined = body.length): Response {
  const headers: Record<string, string> = {}
  if (total !== undefined) {
    headers['content-length'] = String(total)
  }
  return new Response(new Uint8Array(body), { status: 200, headers })
}

/** Response whose body streams in multiple chunks, to exercise progress. */
function streamedResponse(parts: Buffer[]): Response {
  const total = parts.reduce((n, p) => n + p.length, 0)
  const stream = new ReadableStream<Uint8Array>({
    start(controller) {
      for (const p of parts) {
        controller.enqueue(new Uint8Array(p))
      }
      controller.close()
    }
  })
  return new Response(stream, { status: 200, headers: { 'content-length': String(total) } })
}

async function exists(path: string): Promise<boolean> {
  try {
    await stat(path)
    return true
  } catch {
    return false
  }
}

describe('downloadFile', () => {
  let dir: string

  beforeEach(async () => {
    dir = await mkdtemp(join(tmpdir(), 'axo-dl-'))
  })

  afterEach(async () => {
    vi.unstubAllGlobals()
    await rm(dir, { recursive: true, force: true })
  })

  it('writes a hash-verified file on the first attempt', async () => {
    const content = Buffer.from('axo-client-jar-bytes')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(bufferedResponse(content)))
    const dest = join(dir, 'out.jar')

    await downloadFile('https://example/out.jar', dest, sha1(content))

    expect(await readFile(dest)).toEqual(content)
    expect(await exists(`${dest}.part`)).toBe(false)
  })

  it('rejects and cleans up on a hash mismatch', async () => {
    const content = Buffer.from('tampered')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(bufferedResponse(content)))
    const dest = join(dir, 'out.jar')

    await expect(
      downloadFile('https://example/out.jar', dest, 'a'.repeat(40), { attempts: 1 })
    ).rejects.toThrow(/sha1 mismatch/)

    expect(await exists(dest)).toBe(false)
    expect(await exists(`${dest}.part`)).toBe(false)
  })

  it('retries a transient failure and then succeeds', async () => {
    const content = Buffer.from('second-time-lucky')
    const fetchMock = vi
      .fn()
      .mockRejectedValueOnce(new Error('ECONNRESET'))
      .mockResolvedValueOnce(bufferedResponse(content))
    vi.stubGlobal('fetch', fetchMock)
    const onRetry = vi.fn()
    const dest = join(dir, 'out.jar')

    await downloadFile('https://example/out.jar', dest, sha1(content), {
      attempts: 2,
      retryDelayMs: 0,
      onRetry
    })

    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(onRetry).toHaveBeenCalledTimes(1)
    expect(onRetry.mock.calls[0][0]).toMatchObject({ attempt: 1, delayMs: 0 })
    expect(await readFile(dest)).toEqual(content)
  })

  it('doubles the backoff delay between attempts', async () => {
    const content = Buffer.from('third-time-lucky')
    const fetchMock = vi
      .fn()
      .mockRejectedValueOnce(new Error('503'))
      .mockRejectedValueOnce(new Error('503'))
      .mockResolvedValueOnce(bufferedResponse(content))
    vi.stubGlobal('fetch', fetchMock)
    const onRetry = vi.fn()
    const dest = join(dir, 'out.jar')

    await downloadFile('https://example/out.jar', dest, sha1(content), {
      attempts: 3,
      retryDelayMs: 5,
      onRetry
    })

    const delays = onRetry.mock.calls.map((c) => c[0].delayMs)
    expect(delays).toEqual([5, 10])
    expect(await readFile(dest)).toEqual(content)
  })

  it('reports cumulative streamed progress up to the total', async () => {
    const parts = [Buffer.from('aaaa'), Buffer.from('bbbbbb'), Buffer.from('cc')]
    const content = Buffer.concat(parts)
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(streamedResponse(parts)))
    const received: number[] = []
    const dest = join(dir, 'out.bin')

    await downloadFile('https://example/out.bin', dest, sha1(content), {
      onProgress: (p) => received.push(p.received)
    })

    // Monotonically increasing, ending exactly at the total byte count.
    expect(received[received.length - 1]).toBe(content.length)
    for (let i = 1; i < received.length; i++) {
      expect(received[i]).toBeGreaterThan(received[i - 1])
    }
  })

  it('gives up after exhausting all attempts', async () => {
    const fetchMock = vi.fn().mockRejectedValue(new Error('network down'))
    vi.stubGlobal('fetch', fetchMock)
    const dest = join(dir, 'out.jar')

    await expect(
      downloadFile('https://example/out.jar', dest, undefined, { attempts: 3, retryDelayMs: 0 })
    ).rejects.toThrow(/network down/)
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })
})
