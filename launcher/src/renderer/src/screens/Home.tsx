import { useEffect, useState } from 'react'
import type { ManifestInfo } from '../../../shared/types'

export default function HomeScreen(): React.JSX.Element {
  const [manifest, setManifest] = useState<ManifestInfo | null>(null)
  const [manifestError, setManifestError] = useState<string | null>(null)
  const [channel, setChannel] = useState('stable')
  const [versionId, setVersionId] = useState<string | null>(null)

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

  const versions = manifest?.channels[channel]?.versions ?? []

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

      <div className="play-panel">
        <div className="version-row">
          <label>
            Channel
            <select value={channel} onChange={(e) => setChannel(e.target.value)}>
              {Object.keys(manifest?.channels ?? { stable: null }).map((name) => (
                <option key={name} value={name}>
                  {name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Version
            <select value={versionId ?? ''} onChange={(e) => setVersionId(e.target.value)}>
              {versions.map((v) => (
                <option key={v.id} value={v.id}>
                  Minecraft {v.mcVersion}
                </option>
              ))}
            </select>
          </label>
        </div>

        {/* Install + launch pipeline lands in P2-09..P2-13; the button is honest about it. */}
        <button className="primary-button play-button" disabled title="Launching arrives with roadmap tasks P2-09 to P2-13">
          Play
        </button>
        <p className="muted">
          {versions.find((v) => v.id === versionId)?.notes ?? 'Select a version to play.'}
        </p>
      </div>
    </div>
  )
}
