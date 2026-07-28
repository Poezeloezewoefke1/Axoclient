import { describe, expect, it } from 'vitest'
import { OP, activityPayload, decodeFrames, encodeFrame, ipcCandidates } from '../src/main/discord'

describe('encodeFrame', () => {
  it('writes opcode, length and JSON body', () => {
    const frame = encodeFrame(OP.HANDSHAKE, { v: 1 })
    expect(frame.readInt32LE(0)).toBe(0)
    expect(frame.readInt32LE(4)).toBe(frame.length - 8)
    expect(JSON.parse(frame.subarray(8).toString('utf8'))).toEqual({ v: 1 })
  })

  it('uses byte length, not character count, for multi-byte payloads', () => {
    const frame = encodeFrame(OP.FRAME, { state: 'héllo ✨' })
    expect(frame.readInt32LE(4)).toBe(frame.length - 8)
  })
})

describe('decodeFrames', () => {
  it('round-trips a single frame', () => {
    const { frames, rest } = decodeFrames(encodeFrame(OP.FRAME, { cmd: 'SET_ACTIVITY' }))
    expect(frames).toEqual([{ op: OP.FRAME, payload: { cmd: 'SET_ACTIVITY' } }])
    expect(rest).toHaveLength(0)
  })

  it('reads several frames out of one chunk', () => {
    const chunk = Buffer.concat([encodeFrame(OP.FRAME, { a: 1 }), encodeFrame(OP.CLOSE, { b: 2 })])
    const { frames } = decodeFrames(chunk)
    expect(frames.map((f) => f.op)).toEqual([OP.FRAME, OP.CLOSE])
  })

  it('keeps a partial frame as leftover instead of losing it', () => {
    const whole = encodeFrame(OP.FRAME, { hello: 'world' })
    const first = decodeFrames(whole.subarray(0, 10))
    expect(first.frames).toEqual([])
    expect(first.rest).toHaveLength(10)

    const second = decodeFrames(Buffer.concat([first.rest, whole.subarray(10)]))
    expect(second.frames).toEqual([{ op: OP.FRAME, payload: { hello: 'world' } }])
    expect(second.rest).toHaveLength(0)
  })

  it('keeps a header-only remainder', () => {
    const { frames, rest } = decodeFrames(Buffer.alloc(4))
    expect(frames).toEqual([])
    expect(rest).toHaveLength(4)
  })

  it('discards the stream on an absurd declared length', () => {
    const bad = Buffer.alloc(8)
    bad.writeInt32LE(OP.FRAME, 0)
    bad.writeInt32LE(-1, 4)
    expect(decodeFrames(bad).rest).toHaveLength(0)
  })

  it('yields a null payload rather than throwing on malformed JSON', () => {
    const header = Buffer.alloc(8)
    const body = Buffer.from('{oops', 'utf8')
    header.writeInt32LE(OP.FRAME, 0)
    header.writeInt32LE(body.length, 4)
    expect(decodeFrames(Buffer.concat([header, body])).frames).toEqual([
      { op: OP.FRAME, payload: null }
    ])
  })
})

describe('ipcCandidates', () => {
  it('uses named pipes on Windows', () => {
    const paths = ipcCandidates({}, 'win32')
    expect(paths).toHaveLength(10)
    expect(paths[0]).toBe('\\\\?\\pipe\\discord-ipc-0')
  })

  it('prefers XDG_RUNTIME_DIR elsewhere and covers the sandbox paths', () => {
    const paths = ipcCandidates({ XDG_RUNTIME_DIR: '/run/user/1000' }, 'linux')
    expect(paths[0]).toBe('/run/user/1000/discord-ipc-0')
    expect(paths.some((p) => p.includes('com.discordapp.Discord'))).toBe(true)
  })

  it('falls back to /tmp when no runtime dir is set', () => {
    expect(ipcCandidates({}, 'linux')[0]).toBe('/tmp/discord-ipc-0')
  })
})

describe('activityPayload', () => {
  it('builds a SET_ACTIVITY command', () => {
    const payload = activityPayload(42, { details: 'In the menus', state: '1.21.11' }, '1')
    expect(payload.cmd).toBe('SET_ACTIVITY')
    expect(payload.nonce).toBe('1')
    const args = payload.args as { pid: number; activity: Record<string, unknown> }
    expect(args.pid).toBe(42)
    expect(args.activity.details).toBe('In the menus')
    expect(args.activity.state).toBe('1.21.11')
  })

  it('omits timestamps and assets when not supplied', () => {
    const args = activityPayload(1, { details: 'x' }, '1').args as {
      activity: Record<string, unknown>
    }
    expect(args.activity.timestamps).toBeUndefined()
    expect(args.activity.assets).toBeUndefined()
  })

  it('includes a start timestamp when given', () => {
    const args = activityPayload(1, { startTimestamp: 1700000000000 }, '1').args as {
      activity: { timestamps: { start: number } }
    }
    expect(args.activity.timestamps.start).toBe(1700000000000)
  })

  it('sends a null activity to clear presence', () => {
    const args = activityPayload(1, null, '1').args as { activity: unknown }
    expect(args.activity).toBeNull()
  })
})
