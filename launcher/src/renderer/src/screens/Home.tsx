import { useCallback, useEffect, useState } from 'react'
import type { GameProgress, ManifestInfo, SkinInfo, VersionStatus } from '../../../shared/types'
import { formatBytes, formatDuration, formatSpeed } from '../../../shared/format'
import SkinRender from '../components/SkinRender'
import { IconCaret, IconFolder, IconRefresh, IconSpark, IconWrench } from '../components/Icons'

const STAGE_LABELS: Record<GameProgress['stage'], string> = {
  preparing: 'Preparing…',
  java: 'Setting up Java…',
  mods: 'Installing mods…',
  downloading: 'Downloading game…',
  launching: 'Launching…',
  running: 'Running',
  closed: 'Play'
}

const STATE_LABEL: Record<VersionStatus['state'], string> = {
  installed: 'Installed',
  partial: 'Needs repair',
  'not-installed': 'Not installed'
}

/** Static changelog — matches the zero-backend design (no news server). */
const NEWS: { tag: string; title: string; body: string }[] = [
  { tag: 'New', title: 'Skin changer', body: 'Change your Minecraft skin right here in the launcher.' },
  { tag: 'New', title: 'Capes are here', body: 'Pick a Blue, Red, Purple, or Black cape in the in-game menu.' },
  { tag: 'Update', title: 'HUD editor', body: 'Drag your FPS, CPS, and coordinates anywhere on screen.' },
  { tag: 'Update', title: 'Performance', body: 'Sodium and Lithium are bundled for big FPS gains.' }
]

export default function HomeScreen({
  onGoToVersions,
  skin
}: {
  onGoToVersions: () => void
  username: string
  skin: SkinInfo | null
}): React.JSX.Element {
  const [manifest, setManifest] = useState<ManifestInfo | null>(null)
  const [manifestError, setManifestError] = useState<string | null>(null)
  const [channel, setChannel] = useState('stable')
  const [versionId, setVersionId] = useState<string | null>(null)
  const [progress, setProgress] = useState<GameProgress | null>(null)
  const [launchError, setLaunchError] = useState<string | null>(null)
  const [statuses, setStatuses] = useState<VersionStatus[] | null>(null)
  const [repairMsg, setRepairMsg] = useState<string | null>(null)
  const [repairing, setRepairing] = useState(false)
  const [pickerOpen, setPickerOpen] = useState(false)

  const refreshStatuses = useCallback(() => {
    window.axo
      .listVersions()
      .then(setStatuses)
      .catch(() => undefined)
  }, [])

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
    refreshStatuses()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(
    () =>
      window.axo.onGameProgress((p) => {
        setProgress(p)
        if (p.stage === 'closed') {
          refreshStatuses()
        }
        if (p.stage === 'closed' && p.detail?.startsWith('crash')) {
          setLaunchError(
            p.detail === 'crash-boot'
              ? 'Minecraft crashed while starting. Check the game logs (Open logs below) — if it keeps happening, use Repair.'
              : 'Minecraft crashed. Check the game logs via Open logs below.'
          )
        }
      }),
    [refreshStatuses]
  )

  const busy = progress !== null && progress.stage !== 'closed'
  const channelNames = Object.keys(manifest?.channels ?? { stable: null })
  const versions = manifest?.channels[channel]?.versions ?? []
  const selectedStatus = statuses?.find((s) => s.id === versionId) ?? null
  const needsInstall = selectedStatus === null || selectedStatus.state !== 'installed'
  const selectedVersion = versions.find((v) => v.id === versionId)

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

  const repair = (): void => {
    setRepairing(true)
    setRepairMsg('Repairing…')
    window.axo
      .repair()
      .then((r) =>
        setRepairMsg(`Repaired — ${r.downloaded} restored, ${r.kept} intact, ${r.removed} removed`)
      )
      .catch((e: unknown) => setRepairMsg(e instanceof Error ? e.message : 'Repair failed — see logs.'))
      .finally(() => {
        setRepairing(false)
        refreshStatuses()
      })
  }

  const bigLabel = busy && progress ? STAGE_LABELS[progress.stage] : needsInstall ? 'INSTALL & PLAY' : 'LAUNCH'

  return (
    <div className="play-layout">
      <section className="play-stage">
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
        {manifest?.stale && (
          <div className="offline-pill" title="Showing cached version list — network unavailable">
            Offline data
          </div>
        )}

        <div className="skin-hero">
          {skin?.dataUrl ? (
            <SkinRender dataUrl={skin.dataUrl} slim={skin.slim} scale={16} />
          ) : (
            <div className="skin-hero-empty">
              <p className="muted">No skin loaded</p>
            </div>
          )}
        </div>

        <div className="launch-dock">
          <div className="launch-row">
            <button
              className="launch-btn"
              disabled={!versionId || busy || manifest?.forcedUpdate}
              onClick={play}
            >
              <span className="launch-btn-label">{bigLabel}</span>
              <span className="launch-btn-sub">
                {selectedVersion ? `Minecraft ${selectedVersion.mcVersion}` : 'Select a version'}
                {selectedStatus && (
                  <span className={`state-dot state-${selectedStatus.state}`} title={STATE_LABEL[selectedStatus.state]} />
                )}
              </span>
            </button>
            <div className="version-picker">
              <button
                className="version-toggle"
                disabled={busy}
                onClick={() => setPickerOpen((v) => !v)}
                title="Change version"
              >
                <IconCaret size={20} />
              </button>
              {pickerOpen && (
                <>
                  <div className="menu-scrim" onClick={() => setPickerOpen(false)} />
                  <div className="version-menu">
                    {channelNames.length > 1 && (
                      <div className="channel-tabs">
                        {channelNames.map((name) => (
                          <button
                            key={name}
                            className={name === channel ? 'channel-tab active' : 'channel-tab'}
                            onClick={() => {
                              setChannel(name)
                              const def = manifest?.channels[name]?.default
                              if (def) setVersionId(def)
                            }}
                          >
                            {name}
                          </button>
                        ))}
                      </div>
                    )}
                    <div className="version-options">
                      {versions.map((v) => {
                        const st = statuses?.find((s) => s.id === v.id)
                        return (
                          <button
                            key={v.id}
                            className={v.id === versionId ? 'version-option active' : 'version-option'}
                            onClick={() => {
                              setVersionId(v.id)
                              setPickerOpen(false)
                            }}
                          >
                            <span>Minecraft {v.mcVersion}</span>
                            {st && <span className={`state-dot state-${st.state}`} />}
                          </button>
                        )
                      })}
                    </div>
                  </div>
                </>
              )}
            </div>
          </div>

          {busy && progress && progress.received !== undefined && (
            <div className="progress">
              <div className="progress-bar">
                <div
                  className="progress-fill"
                  style={{
                    width:
                      progress.total && progress.total > 0
                        ? `${Math.min(100, (progress.received / progress.total) * 100)}%`
                        : '100%'
                  }}
                />
              </div>
              <div className="progress-stats">
                <span>
                  {formatBytes(progress.received)}
                  {progress.total ? ` / ${formatBytes(progress.total)}` : ''}
                </span>
                <span>
                  {[formatSpeed(progress.bytesPerSecond ?? 0), formatDuration(progress.etaSeconds ?? null)]
                    .filter(Boolean)
                    .join(' · ')}
                </span>
              </div>
            </div>
          )}

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

          <div className="quick-actions">
            <button className="chip-action" onClick={repair} disabled={busy || repairing}>
              <IconWrench /> {repairing ? 'Repairing…' : 'Repair'}
            </button>
            <button className="chip-action" onClick={() => void window.axo.openLogs()}>
              <IconFolder /> Logs
            </button>
            <button className="chip-action" onClick={onGoToVersions} disabled={busy}>
              <IconRefresh /> Versions
            </button>
          </div>
          {repairMsg && <p className="muted repair-msg">{repairMsg}</p>}
        </div>
      </section>

      <aside className="news-panel">
        <div className="news-head">
          <IconSpark />
          <span>News</span>
        </div>
        <div className="news-list">
          {NEWS.map((n) => (
            <article className="news-card" key={n.title}>
              <span className={`news-tag news-tag-${n.tag.toLowerCase()}`}>{n.tag}</span>
              <h3>{n.title}</h3>
              <p>{n.body}</p>
            </article>
          ))}
        </div>
      </aside>
    </div>
  )
}
