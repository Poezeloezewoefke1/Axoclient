import { describe, expect, it } from 'vitest'
import { JVM_PRESETS, recommendRamMb } from '../src/main/system'

describe('recommendRamMb', () => {
  it('leaves the operating system room on small machines', () => {
    // 4 GB machine: half would be 2 GB, and 2 GB headroom leaves 2 GB.
    expect(recommendRamMb(4096)).toBe(2048)
  })

  it('suggests about half of a typical 16 GB machine', () => {
    expect(recommendRamMb(16384)).toBe(8192)
  })

  it('gives 8 GB on an 8 GB machine but never more than it can spare', () => {
    expect(recommendRamMb(8192)).toBe(4096)
  })

  it('never recommends more than 8 GB, however big the machine', () => {
    expect(recommendRamMb(65536)).toBe(8192)
    expect(recommendRamMb(131072)).toBe(8192)
  })

  it('never drops below a playable 2 GB', () => {
    expect(recommendRamMb(2048)).toBe(2048)
    expect(recommendRamMb(1024)).toBe(2048)
  })

  it('always lands on a clean 512 MB step', () => {
    for (const total of [3000, 6000, 10_000, 12_345, 20_000]) {
      expect(recommendRamMb(total) % 512).toBe(0)
    }
  })

  it('falls back to a safe default for nonsense input', () => {
    expect(recommendRamMb(0)).toBe(4096)
    expect(recommendRamMb(-1)).toBe(4096)
    expect(recommendRamMb(Number.NaN)).toBe(4096)
  })
})

describe('JVM_PRESETS', () => {
  it('offers a do-nothing balanced default first', () => {
    expect(JVM_PRESETS[0].id).toBe('balanced')
    expect(JVM_PRESETS[0].args).toBe('')
  })

  it('has unique ids and non-empty labels', () => {
    const ids = JVM_PRESETS.map((p) => p.id)
    expect(new Set(ids).size).toBe(ids.length)
    for (const preset of JVM_PRESETS) {
      expect(preset.label.length).toBeGreaterThan(0)
      expect(preset.description.length).toBeGreaterThan(0)
    }
  })

  it('only contains flags the JVM will accept', () => {
    for (const preset of JVM_PRESETS) {
      for (const arg of preset.args.split(/\s+/).filter(Boolean)) {
        expect(arg.startsWith('-')).toBe(true)
      }
    }
  })
})
