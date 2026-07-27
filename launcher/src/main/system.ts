import { totalmem } from 'node:os'

/**
 * System-aware defaults. Picking memory for the player beats making them
 * guess: too little stutters, too much starves Windows and can be slower
 * than a modest heap.
 */

export const JVM_PRESETS: { id: string; label: string; description: string; args: string }[] = [
  {
    id: 'balanced',
    label: 'Balanced',
    description: 'Minecraft’s normal settings. Safest choice.',
    args: ''
  },
  {
    id: 'performance',
    label: 'Performance',
    description: 'Smoother frame times on most PCs (G1 garbage collector, tuned).',
    args: [
      '-XX:+UseG1GC',
      '-XX:+ParallelRefProcEnabled',
      '-XX:MaxGCPauseMillis=200',
      '-XX:+UnlockExperimentalVMOptions',
      '-XX:+DisableExplicitGC',
      '-XX:G1NewSizePercent=30',
      '-XX:G1MaxNewSizePercent=40',
      '-XX:G1HeapRegionSize=8M',
      '-XX:G1ReservePercent=20',
      '-XX:InitiatingHeapOccupancyPercent=15'
    ].join(' ')
  },
  {
    id: 'low-memory',
    label: 'Low memory',
    description: 'For PCs with 8 GB RAM or less — keeps the heap small and tidy.',
    args: '-XX:+UseSerialGC -XX:MaxGCPauseMillis=100'
  }
]

/**
 * Recommended heap in MiB for a machine with `totalMb` of RAM. Roughly half
 * the machine, always leaving at least 2 GB for the OS, clamped to a range
 * where Minecraft actually behaves — more than ~8 GB rarely helps and makes
 * garbage collection pauses worse.
 */
export function recommendRamMb(totalMb: number): number {
  if (!Number.isFinite(totalMb) || totalMb <= 0) {
    return 4096
  }
  const half = Math.floor(totalMb / 2)
  const leavingHeadroom = Math.min(half, totalMb - 2048)
  const rounded = Math.floor(leavingHeadroom / 512) * 512
  return Math.max(2048, Math.min(8192, rounded))
}

/** Recommended heap for the machine this launcher is running on. */
export function recommendedRamMb(): number {
  return recommendRamMb(Math.floor(totalmem() / 1048576))
}
