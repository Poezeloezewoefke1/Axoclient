import { useCallback, useEffect, useState } from 'react'
import type { ManifestInfo, ScreenshotInfo } from '../../../shared/types'

function formatSize(bytes: number): string {
  return bytes >= 1048576
    ? `${(bytes / 1048576).toFixed(1)} MB`
    : `${Math.max(1, Math.round(bytes / 1024))} KB`
}

/**
 * Screenshots taken in-game, per version. The list is metadata only — a full
 * folder of 4K PNGs would be tens of megabytes to inline — and the image is
 * fetched only when one is opened.
 */
export default function ShotsScreen(): React.JSX.Element {
  const [versions, setVersions] = useState<{ id: string; mcVersion: string }[]>([])
  const [versionId, setVersionId] = useState<string | null>(null)
  const [shots, setShots] = useState<ScreenshotInfo[]>([])
  const [openName, setOpenName] = useState<string | null>(null)
  const [openData, setOpenData] = useState<string | null>(null)
  /** Replaces the filename in the viewer footer after a copy, as feedback. */
  const [copied, setCopied] = useState<string | null>(null)

  const refresh = useCallback((id: string) => {
    window.axo
      .listShots(id)
      .then(setShots)
      .catch(() => setShots([]))
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
      .catch(() => undefined)
  }, [refresh])

  const open = (fileName: string): void => {
    if (!versionId) {
      return
    }
    setOpenName(fileName)
    setOpenData(null)
    setCopied(null)
    void window.axo
      .readShot(versionId, fileName)
      .then(setOpenData)
      .catch(() => setOpenData(null))
  }

  const copy = (fileName: string): void => {
    if (!versionId) {
      return
    }
    void window.axo
      .copyShot(versionId, fileName)
      .then((ok) => setCopied(ok ? 'Copied to clipboard' : 'Could not copy that image'))
      .catch(() => setCopied('Could not copy that image'))
  }

  const remove = (fileName: string): void => {
    if (!versionId || !window.confirm(`Delete ${fileName}?`)) {
      return
    }
    void window.axo
      .deleteShot(versionId, fileName)
      .then(setShots)
      .catch(() => undefined)
    if (openName === fileName) {
      setOpenName(null)
    }
  }

  return (
    <div className="shots-screen">
      <div className="versions-head">
        <h1>Screenshots</h1>
        {versionId && (
          <select
            value={versionId}
            onChange={(e) => {
              setVersionId(e.target.value)
              setOpenName(null)
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

      {shots.length === 0 ? (
        <div className="note-box">
          No screenshots yet. Press <strong>F2</strong> in-game and they&apos;ll show up here.
        </div>
      ) : (
        <div className="shot-grid">
          {shots.map((shot) => (
            <button className="shot-card" key={shot.fileName} onClick={() => open(shot.fileName)}>
              <span className="shot-name" title={shot.fileName}>
                {shot.fileName}
              </span>
              <span className="muted shot-meta">
                {new Date(shot.modifiedAt).toLocaleString()} · {formatSize(shot.sizeBytes)}
              </span>
            </button>
          ))}
        </div>
      )}

      {openName && (
        <div className="modal-backdrop" onClick={() => setOpenName(null)}>
          <div className="shot-viewer" onClick={(e) => e.stopPropagation()}>
            {openData ? (
              <img src={openData} alt={openName} />
            ) : (
              <p className="muted">Loading…</p>
            )}
            <div className="shot-viewer-actions">
              <span className="muted">{copied ?? openName}</span>
              <button className="chip-action" onClick={() => copy(openName)}>
                Copy image
              </button>
              <button
                className="chip-action"
                onClick={() => versionId && void window.axo.revealShot(versionId, openName)}
              >
                Show in folder
              </button>
              <button className="link-button danger" onClick={() => remove(openName)}>
                Delete
              </button>
              <button className="link-button" onClick={() => setOpenName(null)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
