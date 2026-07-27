import { useCallback, useEffect, useState } from 'react'
import type { ManifestInfo, UserMod } from '../../../shared/types'

/**
 * Player-added mods, per version. Axo's own mods (the client jar, Sodium,
 * Lithium) aren't listed — they're managed by the manifest and Repair, and
 * showing them here would invite people to delete something the launcher
 * immediately puts back.
 */
export default function ModsScreen(): React.JSX.Element {
  const [versions, setVersions] = useState<{ id: string; mcVersion: string }[]>([])
  const [versionId, setVersionId] = useState<string | null>(null)
  const [mods, setMods] = useState<UserMod[]>([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback((id: string) => {
    window.axo
      .listMods(id)
      .then(setMods)
      .catch(() => setMods([]))
  }, [])

  useEffect(() => {
    window.axo
      .getManifest()
      .then((info: ManifestInfo) => {
        const all = Object.values(info.channels).flatMap((c) => c.versions)
        setVersions(all.map((v) => ({ id: v.id, mcVersion: v.mcVersion })))
        const first = all[0]?.id ?? null
        setVersionId(first)
        if (first) {
          refresh(first)
        }
      })
      .catch(() => setError('Could not load the version list.'))
  }, [refresh])

  const run = (action: Promise<UserMod[]>): void => {
    setBusy(true)
    setError(null)
    action
      .then(setMods)
      .catch((e: unknown) => setError(e instanceof Error ? e.message : 'That did not work.'))
      .finally(() => setBusy(false))
  }

  return (
    <div className="mods-screen">
      <div className="versions-head">
        <h1>Mods</h1>
        {versionId && (
          <select
            value={versionId}
            onChange={(e) => {
              setVersionId(e.target.value)
              refresh(e.target.value)
            }}
          >
            {versions.map((v) => (
              <option key={v.id} value={v.id}>
                Minecraft {v.mcVersion}
              </option>
            ))}
          </select>
        )}
      </div>
      <p className="muted">
        Add your own mods for this version. They stay put when the launcher repairs or updates —
        only Axo&apos;s own files are managed for you.
      </p>

      {error && <div className="error-banner">{error}</div>}

      <div className="quick-actions" style={{ justifyContent: 'flex-start', marginTop: 16 }}>
        <button
          className="btn-secondary"
          disabled={busy || !versionId}
          onClick={() => versionId && run(window.axo.addMods(versionId))}
        >
          {busy ? 'Working…' : 'Add mods…'}
        </button>
      </div>

      {mods.length === 0 ? (
        <div className="note-box">
          No mods added yet. Press <strong>Add mods…</strong> and pick any <code>.jar</code> file.
          Make sure it&apos;s built for this Minecraft version and for Fabric.
        </div>
      ) : (
        <div className="version-list">
          {mods.map((mod) => (
            <div className="version-row-card" key={mod.fileName}>
              <div className="version-info">
                <div className="version-title">
                  {mod.fileName.replace(/\.jar$/, '')}
                  <span className={`state-badge ${mod.enabled ? 'state-installed' : 'state-not-installed'}`}>
                    {mod.enabled ? 'On' : 'Off'}
                  </span>
                </div>
                <div className="muted version-sub">{mod.fileName}</div>
              </div>
              <div className="version-actions">
                <button
                  className="btn-secondary"
                  disabled={busy || !versionId}
                  onClick={() =>
                    versionId && run(window.axo.setModEnabled(versionId, mod.fileName, !mod.enabled))
                  }
                >
                  {mod.enabled ? 'Turn off' : 'Turn on'}
                </button>
                <button
                  className="link-button danger"
                  disabled={busy || !versionId}
                  onClick={() => {
                    if (versionId && window.confirm(`Delete ${mod.fileName}?`)) {
                      run(window.axo.removeMod(versionId, mod.fileName))
                    }
                  }}
                >
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="note-box">
        If the game stops starting after adding a mod, turn it off here — that&apos;s almost always
        the cause.
      </div>
    </div>
  )
}
