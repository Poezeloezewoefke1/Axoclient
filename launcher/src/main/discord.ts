import { connect, type Socket } from 'node:net'
import { platform } from 'node:os'
import { join } from 'node:path'

/**
 * Discord Rich Presence — "Playing Axo Client" on your profile.
 *
 * Deliberately dependency-free. The usual libraries pull in a native addon
 * that has to be rebuilt for every Electron ABI, which is a recurring source
 * of "works on my machine" breakage. Discord's local IPC is just a Unix
 * socket (or Windows named pipe) speaking a trivial framed-JSON protocol, so
 * we implement it directly: node:net only, nothing to compile.
 *
 * If Discord isn't running, nothing here throws — presence is a nice-to-have
 * and must never be able to stop a launch.
 */

/** Opcodes from Discord's IPC framing. Only these three matter to us. */
export const OP = {
  HANDSHAKE: 0,
  FRAME: 1,
  CLOSE: 2
} as const

export interface PresenceActivity {
  /** Top line, e.g. "Playing on Hypixel". */
  details?: string
  /** Second line, e.g. "1.21.11". */
  state?: string
  /** Epoch millis; Discord renders this as an "elapsed" timer. */
  startTimestamp?: number
  largeImageKey?: string
  largeImageText?: string
}

/**
 * Frame a payload the way Discord expects: 32-bit LE opcode, 32-bit LE
 * length, then UTF-8 JSON.
 */
export function encodeFrame(op: number, payload: unknown): Buffer {
  const body = Buffer.from(JSON.stringify(payload), 'utf8')
  const header = Buffer.alloc(8)
  header.writeInt32LE(op, 0)
  header.writeInt32LE(body.length, 4)
  return Buffer.concat([header, body])
}

export interface DecodedFrame {
  op: number
  payload: unknown
}

/**
 * Pull every complete frame out of a buffer.
 *
 * Returns the leftover bytes too: the socket hands us arbitrary chunks, so a
 * frame can and will arrive split down the middle. The caller keeps `rest`
 * and prepends it to the next chunk.
 */
export function decodeFrames(buffer: Buffer): { frames: DecodedFrame[]; rest: Buffer } {
  const frames: DecodedFrame[] = []
  let offset = 0

  while (buffer.length - offset >= 8) {
    const op = buffer.readInt32LE(offset)
    const length = buffer.readInt32LE(offset + 4)
    // A negative or absurd length means the stream is desynced; drop it all
    // rather than trying to allocate our way out.
    if (length < 0 || length > 64 * 1024 * 1024) {
      return { frames, rest: Buffer.alloc(0) }
    }
    if (buffer.length - offset - 8 < length) break

    const body = buffer.subarray(offset + 8, offset + 8 + length).toString('utf8')
    let payload: unknown = null
    try {
      payload = JSON.parse(body)
    } catch {
      payload = null
    }
    frames.push({ op, payload })
    offset += 8 + length
  }

  return { frames, rest: buffer.subarray(offset) }
}

/**
 * Candidate socket paths, in the order Discord itself probes them.
 * discord-ipc-0 through -9 covers stable/PTB/Canary running side by side.
 */
export function ipcCandidates(env: NodeJS.ProcessEnv = process.env, os = platform()): string[] {
  const names = Array.from({ length: 10 }, (_, i) => `discord-ipc-${i}`)
  if (os === 'win32') {
    return names.map((name) => `\\\\?\\pipe\\${name}`)
  }
  // Flatpak/Snap Discord hides the socket a level or two deeper.
  const base = env.XDG_RUNTIME_DIR || env.TMPDIR || env.TMP || '/tmp'
  const roots = [base, join(base, 'app', 'com.discordapp.Discord'), join(base, 'snap.discord')]
  return roots.flatMap((root) => names.map((name) => join(root, name)))
}

/** Discord's ACTIVITY_SET command payload. Extracted so it can be tested. */
export function activityPayload(
  pid: number,
  activity: PresenceActivity | null,
  nonce: string
): Record<string, unknown> {
  return {
    cmd: 'SET_ACTIVITY',
    nonce,
    args: {
      pid,
      activity: activity
        ? {
            details: activity.details,
            state: activity.state,
            timestamps: activity.startTimestamp ? { start: activity.startTimestamp } : undefined,
            assets: activity.largeImageKey
              ? { large_image: activity.largeImageKey, large_text: activity.largeImageText }
              : undefined
          }
        : null
    }
  }
}

export class DiscordPresence {
  private socket: Socket | null = null
  private connected = false
  private pending: PresenceActivity | null = null
  private buffer = Buffer.alloc(0)
  private nonce = 0

  constructor(private readonly clientId: string) {}

  /** Best-effort connect. Resolves false when Discord isn't reachable. */
  async connect(): Promise<boolean> {
    if (this.connected) return true
    if (!this.clientId) return false

    for (const path of ipcCandidates()) {
      const socket = await openSocket(path)
      if (!socket) continue

      this.socket = socket
      socket.on('data', (chunk) => this.onData(chunk))
      socket.on('error', () => this.reset())
      socket.on('close', () => this.reset())
      socket.write(encodeFrame(OP.HANDSHAKE, { v: 1, client_id: this.clientId }))
      this.connected = true
      if (this.pending) this.setActivity(this.pending)
      return true
    }
    return false
  }

  setActivity(activity: PresenceActivity | null): void {
    this.pending = activity
    if (!this.connected || !this.socket) return
    try {
      this.nonce += 1
      this.socket.write(
        encodeFrame(OP.FRAME, activityPayload(process.pid, activity, String(this.nonce)))
      )
    } catch {
      this.reset()
    }
  }

  clear(): void {
    this.setActivity(null)
  }

  close(): void {
    try {
      this.socket?.end()
    } catch {
      // Already gone — nothing to do.
    }
    this.reset()
  }

  isConnected(): boolean {
    return this.connected
  }

  private onData(chunk: Buffer): void {
    // Discord's replies are only interesting for keeping the stream in sync;
    // we never act on them, but we must consume whole frames regardless.
    const { rest } = decodeFrames(Buffer.concat([this.buffer, chunk]))
    // Copy rather than hold a view onto the concatenated chunk, so the larger
    // buffer can be collected between packets.
    this.buffer = Buffer.from(rest)
  }

  private reset(): void {
    this.connected = false
    this.socket = null
    this.buffer = Buffer.alloc(0)
  }
}

/** Resolve to a connected socket, or null if this path isn't listening. */
function openSocket(path: string): Promise<Socket | null> {
  return new Promise((resolve) => {
    let settled = false
    const done = (value: Socket | null): void => {
      if (settled) return
      settled = true
      resolve(value)
    }

    const socket = connect(path)
    socket.once('connect', () => done(socket))
    socket.once('error', () => {
      socket.destroy()
      done(null)
    })
    // Discord answers instantly when it's there; don't hang the launcher.
    setTimeout(() => {
      if (!settled) {
        socket.destroy()
        done(null)
      }
    }, 400).unref?.()
  })
}
