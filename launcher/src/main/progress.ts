/**
 * Download rate + ETA estimation (roadmap P2-13 "show download speed / time
 * remaining"). Pure and side-effect free so it unit-tests without any I/O.
 * Uses a sliding time window so the reported speed reacts to real changes
 * instead of averaging over the whole transfer.
 */

interface Sample {
  t: number // ms timestamp
  bytes: number // cumulative bytes received at t
}

export class RateTracker {
  private samples: Sample[] = []

  constructor(private readonly windowMs = 3000) {}

  /** Record cumulative bytes received at time `now` (ms). */
  update(cumulativeBytes: number, now: number): void {
    this.samples.push({ t: now, bytes: cumulativeBytes })
    const cutoff = now - this.windowMs
    while (this.samples.length > 2 && this.samples[0].t < cutoff) {
      this.samples.shift()
    }
  }

  /** Bytes per second over the current window, or 0 with too little data. */
  bytesPerSecond(): number {
    if (this.samples.length < 2) {
      return 0
    }
    const first = this.samples[0]
    const last = this.samples[this.samples.length - 1]
    const dt = (last.t - first.t) / 1000
    if (dt <= 0) {
      return 0
    }
    const rate = (last.bytes - first.bytes) / dt
    return rate > 0 ? rate : 0
  }

  /** Seconds until `totalBytes` at the current rate, or null when unknowable. */
  etaSeconds(totalBytes: number | undefined): number | null {
    if (!totalBytes || totalBytes <= 0) {
      return null
    }
    const rate = this.bytesPerSecond()
    if (rate <= 0) {
      return null
    }
    const last = this.samples[this.samples.length - 1]
    const remaining = totalBytes - last.bytes
    if (remaining <= 0) {
      return 0
    }
    return remaining / rate
  }
}
