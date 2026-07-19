export default function SettingsScreen(): React.JSX.Element {
  // Real settings (RAM slider, install dir, JVM args) land in P2-04 / P5-06.
  return (
    <div className="settings-screen">
      <h1>Settings</h1>
      <div className="settings-group">
        <h2>Game</h2>
        <p className="muted">Memory, install location, and JVM options arrive with roadmap task P2-04.</p>
      </div>
      <div className="settings-group">
        <h2>About</h2>
        <p className="muted">
          Axo Launcher — updates are checked automatically once the update system (Phase 3) ships.
        </p>
      </div>
    </div>
  )
}
