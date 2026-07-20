import { useEffect, useState } from 'react'
import type { GameProgress, ManifestInfo } from '../../../shared/types'

const STAGE_LABELS: Record<GameProgress['stage'], string> = {
  preparing: 'Preparing…',
  java: 'Setting up Java…',
  mods: 'Installing mods…',
  downloading: 'Downloading game…',
  launching: 'Launching…',
  running: 'Running',
  closed: 'Play'
}

export default function HomeScreen(): React.JSX.Element {
  const [manifest, setManifest] = useState<ManifestInfo | null>(null)
  const [manifestError, setManifestError] = useState<string | null>(null)
  const [channel, setChannel] = useState('stable')
  const [versionId, setVersionId] = useState<string | null>(null)
  const [progress, setProgress] = useState<GameProgress | null>(null)
  const [launchError, setLaunchError] = useState<string | null>(null)

  useEffect(() => {
    window.axo
      .getManifest()
      .then((info) => {
        setManifest(info)
        const chan = info.channels[channel] ?? Object.values(info.channels)[0]
        if (chan) {
          setVersionId(chan.default)
        }
      })
      .catch((e: unknown) => {
        setManifestError(e instanceof Error ? e.message : 'Could not load version list.')
      })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => window.axo.onGameProgress(setProgress), [])

  const busy = progress !== null && progress.stage !== 'closed'
  const versions = manifest?.channels[channel]?.versions ?? []

  const play = (): void => {
    if (!versionId) {
      return
    }
    setLaunchError(null)
    setProgress({ stage: 'preparing' })
    window.axo.launch(versionId).catch((e: unknown) => {
      setLaunchError(e instanceof Error ? e.message : 'Launch failed — see logs.')
      setProgress(null)
    })
  }

  return (
    <div className="home-screen">
      <header className="home-header">
        <h1>Ready to play</h1>
        {manifest?.stale && (
          <span className="stale-badge" title="Showing cached version list — network unavailable">
            offline data
          </span>
        )}
      </header>

      {manifestError && <div className="error-banner">{manifestError}</div>}
      {manifest?.forcedUpdate && (
        <div className="error-banner">
          This version of Axo Launcher is too old to play. An update will install automatically —
          or download the latest from the website.
        </div>
      )}
      {launchError && (
        <div className="error-banner">
          {launchError}
          <button className="link-button" onClick={play}>
            Retry
          </button>
        </div>
      )}

      <div className="play-panel">
        <div className="version-row">
          <label>
            Channel
            <select
              value={channel}
              disabled={busy}
              onChange={(e) => setChannel(e.target.value)}
            >
              {Object.keys(manifest?.channels ?? { stable: null }).map((name) => (
                <option key={name} value={name}>
                  {name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Version
            <select
              value={versionId ?? ''}
              disabled={busy}
              onChange={(e) => setVersionId(e.target.value)}
            >
              {versions.map((v) => (
                <option key={v.id} value={v.id}>
                  Minecraft {v.mcVersion}
                </option>
              ))}
            </select>
          </label>
        </div>

        <button
          className="primary-button play-button"
          disabled={!versionId || busy || manifest?.forcedUpdate}
          onClick={play}
        >
          {busy && progress ? STAGE_LABELS[progress.stage] : 'Play'}
        </button>
        {progress?.stage === 'running' && (
          <button
            className="link-button"
            onClick={() => {
              if (window.confirm('Force close Minecraft? Unsaved progress will be lost.')) {
                void window.axo.forceClose()
              }
            }}
          >
            Force close game
          </button>
        )}
        <p className="muted">
          {busy && progress?.detail
            ? progress.detail
            : versions.find((v) => v.id === versionId)?.notes ?? 'Select a version to play.'}
        </p>
      </div>
    </div>
  )
}
