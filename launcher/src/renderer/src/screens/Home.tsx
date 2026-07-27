import { useCallback, useEffect, useState } from 'react'
import type {
  CrashDiagnosis,
  GameProgress,
  ManifestInfo,
  NewsItem,
  SkinInfo,
  VersionStatus
} from '../../../shared/types'
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

/** Shown until the real releases load (or if the fetch fails offline). */
const NEWS_PLACEHOLDER: NewsItem[] = [
  {
    tag: 'Release',
    title: 'Welcome to Axo Client',
    body: 'Release notes appear here once the first version is published on GitHub.'
  }
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
  const [news, setNews] = useState<NewsItem[]>(NEWS_PLACEHOLDER)
  const [crash, setCrash] = useState<CrashDiagnosis | null>(null)

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
    window.axo
      .getNews()
      .then((items) => {
        if (items.length > 0) {
          setNews(items)
        }
      })
      .catch(() => undefined)
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
              ? 'Minecraft crashed while starting.'
              : 'Minecraft crashed.'
          )
          // Ask the main process to read and explain the crash report.
          if (versionId) {
            window.axo
              .getCrashReport(versionId)
              .then(setCrash)
              .catch(() => setCrash(null))
          }
        }
      }),
    // versionId is read inside the handler, so the listener must be rebound
    // when the player switches version — otherwise we'd fetch the crash
    // report for whichever version was selected when the app started.
    [refreshStatuses, versionId]
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
    setCrash(null)
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
        {launchError && !crash && (
          <div className="error-banner">
            {launchError}
            <button className="link-button" onClick={play}>
              Retry
            </button>
          </div>
        )}
        {crash && (
          <div className="crash-card">
            <div className="crash-head">
              <span className="crash-icon">⚠️</span>
              <strong>{crash.summary}</strong>
            </div>
            <p className="crash-advice">{crash.advice}</p>
            <div className="crash-actions">
              <button className="btn-secondary" onClick={play}>
                Try again
              </button>
              <button className="link-button" onClick={repair} disabled={repairing}>
                {repairing ? 'Repairing…' : 'Repair install'}
              </button>
              <button className="link-button" onClick={() => void window.axo.openLogs()}>
                Open logs
              </button>
            </div>
            {crash.technical && (
              <details className="crash-details">
                <summary>Technical details</summary>
                <code>{crash.technical}</code>
              </details>
            )}
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
          {news.map((n) => (
            <article className="news-card" key={`${n.title}-${n.date ?? ''}`}>
              <span className={`news-tag news-tag-${n.tag.toLowerCase()}`}>{n.tag}</span>
              <h3>{n.title}</h3>
              {n.body && <p>{n.body}</p>}
              {n.date && (
                <p className="news-date">{new Date(n.date).toLocaleDateString()}</p>
              )}
            </article>
          ))}
        </div>
      </aside>
    </div>
  )
}
