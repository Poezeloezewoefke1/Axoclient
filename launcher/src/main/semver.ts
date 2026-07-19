/**
 * Minimal semver comparison for update gating (P3-03). Only X.Y.Z is
 * supported — that is all our versioning scheme (docs/releasing.md) emits.
 */

export function parseSemver(version: string): [number, number, number] | null {
  const match = /^(\d+)\.(\d+)\.(\d+)/.exec(version.trim())
  if (!match) {
    return null
  }
  return [Number(match[1]), Number(match[2]), Number(match[3])]
}

/** -1: a < b · 0: equal · 1: a > b. Unparseable versions compare as 0.0.0. */
export function compareSemver(a: string, b: string): -1 | 0 | 1 {
  const pa = parseSemver(a) ?? [0, 0, 0]
  const pb = parseSemver(b) ?? [0, 0, 0]
  for (let i = 0; i < 3; i++) {
    if (pa[i] !== pb[i]) {
      return pa[i] < pb[i] ? -1 : 1
    }
  }
  return 0
}
