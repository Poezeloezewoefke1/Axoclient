import { useEffect, useRef } from 'react'

/**
 * Draws a Minecraft character from a 64×64 skin texture on a pixelated canvas —
 * no external services (skin arrives as a data URL from the main process, so it
 * works under the strict CSP). `view="body"` composites the full front-facing
 * body (base + hat/jacket/sleeve/pants overlays, slim-aware); `view="head"`
 * draws just the head + hat overlay for small avatars.
 */
export default function SkinRender({
  dataUrl,
  slim,
  scale = 14,
  view = 'body'
}: {
  dataUrl: string | null
  slim: boolean
  scale?: number
  view?: 'body' | 'head'
}): React.JSX.Element {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const unitsW = view === 'head' ? 8 : 16
  const unitsH = view === 'head' ? 8 : 32

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) {
      return
    }
    const ctx = canvas.getContext('2d')
    if (!ctx) {
      return
    }
    ctx.imageSmoothingEnabled = false
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    if (!dataUrl) {
      return
    }

    const img = new Image()
    img.onload = () => {
      const armW = slim ? 3 : 4
      const part = (sx: number, sy: number, sw: number, sh: number, dx: number, dy: number): void => {
        ctx.drawImage(img, sx, sy, sw, sh, dx * scale, dy * scale, sw * scale, sh * scale)
      }
      if (view === 'head') {
        part(8, 8, 8, 8, 0, 0) // head
        part(40, 8, 8, 8, 0, 0) // hat overlay
        return
      }
      // Base layer.
      part(8, 8, 8, 8, 4, 0) // head
      part(20, 20, 8, 12, 4, 8) // body
      part(44, 20, armW, 12, 4 - armW, 8) // right arm
      part(36, 52, armW, 12, 12, 8) // left arm
      part(4, 20, 4, 12, 4, 20) // right leg
      part(20, 52, 4, 12, 8, 20) // left leg
      // Overlay layer (hat / jacket / sleeves / pants).
      part(40, 8, 8, 8, 4, 0)
      part(20, 36, 8, 12, 4, 8)
      part(44, 36, armW, 12, 4 - armW, 8)
      part(52, 52, armW, 12, 12, 8)
      part(4, 36, 4, 12, 4, 20)
      part(4, 52, 4, 12, 8, 20)
    }
    img.src = dataUrl
  }, [dataUrl, slim, scale, view])

  return (
    <canvas
      ref={canvasRef}
      width={unitsW * scale}
      height={unitsH * scale}
      className="skin-canvas"
      style={{ imageRendering: 'pixelated' }}
    />
  )
}
