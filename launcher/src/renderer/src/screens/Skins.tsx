import { useEffect, useState } from 'react'
import type { SkinInfo } from '../../../shared/types'
import SkinRender from '../components/SkinRender'

/**
 * View and change the signed-in account's Minecraft skin. Reading is public;
 * changing uploads a 64×64 PNG to the Minecraft Services API via the main
 * process (which holds the access token — it never reaches the renderer).
 */
export default function SkinsScreen({
  skin,
  onChanged
}: {
  skin: SkinInfo | null
  onChanged: () => void
}): React.JSX.Element {
  const [variant, setVariant] = useState<'classic' | 'slim'>(skin?.slim ? 'slim' : 'classic')
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setVariant(skin?.slim ? 'slim' : 'classic')
  }, [skin?.slim])

  const change = (): void => {
    setBusy(true)
    setMessage(null)
    setError(null)
    window.axo
      .applySkin(variant)
      .then((dataUrl) => {
        if (dataUrl === null) {
          // User cancelled the file picker.
          return
        }
        setMessage('Skin updated! It may take a minute to show in-game.')
        onChanged()
      })
      .catch((e: unknown) => setError(e instanceof Error ? e.message : 'Could not change skin.'))
      .finally(() => setBusy(false))
  }

  return (
    <div className="skins-screen">
      <div className="skins-preview">
        <div className="skin-hero">
          {skin?.dataUrl ? (
            <SkinRender dataUrl={skin.dataUrl} slim={variant === 'slim'} scale={16} />
          ) : (
            <div className="skin-hero-empty">
              <p className="muted">No skin loaded</p>
            </div>
          )}
        </div>
      </div>

      <div className="skins-controls">
        <h2>Your skin</h2>
        <p className="muted">Change the skin on your Microsoft account. Everyone sees it in-game.</p>

        <div className="field">
          <span className="field-title">Arm style</span>
          <div className="segmented">
            <button
              className={variant === 'classic' ? 'seg active' : 'seg'}
              onClick={() => setVariant('classic')}
            >
              Classic (4px)
            </button>
            <button
              className={variant === 'slim' ? 'seg active' : 'seg'}
              onClick={() => setVariant('slim')}
            >
              Slim (3px)
            </button>
          </div>
        </div>

        <button className="primary-button" onClick={change} disabled={busy}>
          {busy ? 'Uploading…' : 'Choose skin PNG…'}
        </button>

        {message && <p className="skins-ok">{message}</p>}
        {error && <p className="error-text">{error}</p>}

        <p className="muted skins-note">
          Pick a 64×64 PNG skin file. Need one? Grab a skin from any skin website and save the PNG,
          then choose it here.
        </p>
      </div>
    </div>
  )
}
