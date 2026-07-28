import { useEffect, useState } from 'react'
import type { SavedSkin, SkinInfo } from '../../../shared/types'
import SkinRender from '../components/SkinRender'

/**
 * View and change the signed-in account's Minecraft skin, plus a local
 * library of saved skins. Reading is public; changing uploads a 64×64 PNG to
 * the Minecraft Services API via the main process (which holds the access
 * token — it never reaches the renderer).
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
  const [library, setLibrary] = useState<SavedSkin[]>([])

  useEffect(() => {
    setVariant(skin?.slim ? 'slim' : 'classic')
  }, [skin?.slim])

  useEffect(() => {
    void window.axo
      .listSkins()
      .then(setLibrary)
      .catch(() => setLibrary([]))
  }, [])

  const run = (action: Promise<unknown>, okMessage?: string): void => {
    setBusy(true)
    setMessage(null)
    setError(null)
    action
      .then(() => {
        if (okMessage) {
          setMessage(okMessage)
        }
      })
      .catch((e: unknown) => setError(e instanceof Error ? e.message : 'That did not work.'))
      .finally(() => setBusy(false))
  }

  const change = (): void => {
    setBusy(true)
    setMessage(null)
    setError(null)
    window.axo
      .applySkin(variant)
      .then((dataUrl) => {
        if (dataUrl === null) {
          return // cancelled the file picker
        }
        setMessage('Skin updated! It may take a minute to show in-game.')
        onChanged()
      })
      .catch((e: unknown) => setError(e instanceof Error ? e.message : 'Could not change skin.'))
      .finally(() => setBusy(false))
  }

  const saveToLibrary = (): void => {
    const name = window.prompt('Name this skin', 'My skin')
    if (name === null) {
      return
    }
    run(
      window.axo.saveSkinFile(name, variant === 'slim').then(setLibrary),
      'Saved to your library.'
    )
  }

  const saveCurrent = (): void => {
    const name = window.prompt('Name your current skin', 'Current skin')
    if (name === null) {
      return
    }
    run(window.axo.saveCurrentSkin(name).then(setLibrary), 'Current skin saved.')
  }

  const wear = (saved: SavedSkin): void => {
    run(
      window.axo.wearSkin(saved.id, saved.slim).then(() => onChanged()),
      `Now wearing "${saved.name}".`
    )
  }

  const remove = (saved: SavedSkin): void => {
    if (!window.confirm(`Delete "${saved.name}" from your library?`)) {
      return
    }
    run(window.axo.deleteSkin(saved.id).then(setLibrary))
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
          {busy ? 'Working…' : 'Choose skin PNG…'}
        </button>

        <div className="quick-actions" style={{ justifyContent: 'flex-start' }}>
          <button className="chip-action" onClick={saveToLibrary} disabled={busy}>
            Save a PNG to library
          </button>
          <button className="chip-action" onClick={saveCurrent} disabled={busy || !skin?.dataUrl}>
            Save current skin
          </button>
        </div>

        {message && <p className="skins-ok">{message}</p>}
        {error && <p className="error-text">{error}</p>}

        <div className="field">
          <span className="field-title">Your library</span>
          {library.length === 0 ? (
            <p className="muted">
              Nothing saved yet. Save a skin here and swapping later takes one click.
            </p>
          ) : (
            <div className="skin-library">
              {library.map((saved) => (
                <div className="skin-tile" key={saved.id}>
                  <SkinRender dataUrl={saved.dataUrl} slim={saved.slim} scale={3} view="head" />
                  <span className="skin-tile-name" title={saved.name}>
                    {saved.name}
                  </span>
                  <div className="skin-tile-actions">
                    <button className="link-button" disabled={busy} onClick={() => wear(saved)}>
                      Wear
                    </button>
                    <button
                      className="link-button danger"
                      disabled={busy}
                      onClick={() => remove(saved)}
                    >
                      Delete
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <p className="muted skins-note">
          Pick a 64×64 PNG skin file. Need one? Grab a skin from any skin website and save the PNG,
          then choose it here.
        </p>
      </div>
    </div>
  )
}
