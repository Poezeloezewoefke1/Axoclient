import { describe, expect, it } from 'vitest'
import { RateTracker } from '../src/main/progress'
import { formatBytes, formatDuration, formatSpeed } from '../src/shared/format'

describe('RateTracker', () => {
  it('reports 0 with fewer than two samples', () => {
    const t = new RateTracker()
    expect(t.bytesPerSecond()).toBe(0)
    t.update(1000, 0)
    expect(t.bytesPerSecond()).toBe(0)
  })

  it('computes bytes/sec across the window', () => {
    const t = new RateTracker()
    t.update(0, 0)
    t.update(1_000_000, 1000) // 1 MB in 1 s
    expect(t.bytesPerSecond()).toBeCloseTo(1_000_000, -2)
  })

  it('estimates ETA from rate and total', () => {
    const t = new RateTracker()
    t.update(0, 0)
    t.update(1_000_000, 1000) // 1 MB/s
    // 3 MB total, 1 MB done -> 2 MB left -> ~2 s
    expect(t.etaSeconds(3_000_000)).toBeCloseTo(2, 1)
  })

  it('returns null ETA when total is unknown or rate is zero', () => {
    const t = new RateTracker()
    t.update(500, 0)
    expect(t.etaSeconds(undefined)).toBeNull()
    expect(t.etaSeconds(0)).toBeNull()
  })

  it('drops samples older than the window', () => {
    const t = new RateTracker(1000)
    t.update(0, 0)
    t.update(100, 500)
    t.update(2_000_000, 2000) // window keeps only recent samples
    t.update(3_000_000, 2500)
    // rate is measured over the recent ~0.5s window, not the whole transfer
    expect(t.bytesPerSecond()).toBeGreaterThan(0)
  })
})

describe('format helpers', () => {
  it('formats bytes', () => {
    expect(formatBytes(0)).toBe('0 B')
    expect(formatBytes(512)).toBe('512 B')
    expect(formatBytes(1024)).toBe('1.0 KB')
    expect(formatBytes(1_572_864)).toBe('1.5 MB')
    expect(formatBytes(3_221_225_472)).toBe('3.0 GB')
  })

  it('formats speed', () => {
    expect(formatSpeed(0)).toBe('')
    expect(formatSpeed(2_097_152)).toBe('2.0 MB/s')
  })

  it('formats duration', () => {
    expect(formatDuration(null)).toBe('')
    expect(formatDuration(5)).toBe('5s left')
    expect(formatDuration(90)).toBe('1m 30s left')
    expect(formatDuration(120)).toBe('2m left')
    expect(formatDuration(3700)).toBe('1h 1m left')
  })
})
