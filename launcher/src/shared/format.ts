/** Human-readable formatting shared by main + renderer. Pure, unit-tested. */

export function formatBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 0) {
    return '0 B'
  }
  const units = ['B', 'KB', 'MB', 'GB']
  let value = bytes
  let unit = 0
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024
    unit++
  }
  const digits = value < 10 && unit > 0 ? 1 : 0
  return `${value.toFixed(digits)} ${units[unit]}`
}

export function formatSpeed(bytesPerSecond: number): string {
  if (!bytesPerSecond || bytesPerSecond <= 0) {
    return ''
  }
  return `${formatBytes(bytesPerSecond)}/s`
}

export function formatDuration(seconds: number | null): string {
  if (seconds === null || !Number.isFinite(seconds) || seconds < 0) {
    return ''
  }
  const s = Math.ceil(seconds)
  if (s < 60) {
    return `${s}s left`
  }
  const m = Math.floor(s / 60)
  const rem = s % 60
  if (m < 60) {
    return rem ? `${m}m ${rem}s left` : `${m}m left`
  }
  const h = Math.floor(m / 60)
  return `${h}h ${m % 60}m left`
}
