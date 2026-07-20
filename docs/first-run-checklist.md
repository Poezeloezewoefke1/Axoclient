# First-Run Checklist (your Windows testing session)

Work through this top-to-bottom. Anything that fails: copy the error + the relevant log and report it — fixes land from the remote side.

## 0. One-time prerequisites

- [ ] **Make the GitHub repo public** (Settings → General → Danger Zone → Change visibility). Without this, the launcher's manifest fetch and release downloads 404 for unauthenticated requests — nothing remote will work.
- [ ] Install **JDK 21** (Temurin: https://adoptium.net) and **Node 20+** (https://nodejs.org).
- [ ] `git clone` the repo and check out branch `claude/axo-client-architecture-ih525q`.

## 1. Client in dev (closes M1 — roadmap P1-01)

```
cd client
gradlew build
gradlew runClient
```

- [ ] Build green, dev Minecraft 1.21.11 reaches the title screen.
- [ ] Create/join a world: FPS counter (top-left), coordinates, CPS counter visible; keystrokes panel on the middle-left.
- [ ] **Right Shift** opens the Axo settings screen; toggle a module off/on — takes effect immediately.
- [ ] Restart the client — toggles persisted (config/axoclient.json).
- [ ] Hold **C** — zoom in/out; release restores FOV exactly.
- [ ] Enable Fullbright in a dark cave — bright; disable — back to normal.
- [ ] Edit `config/axoclient.json`: set `"hud_anchor": "BOTTOM_RIGHT"` under `modules.fps_hud` → restart → FPS counter moved.

## 2. Launcher in dev

```
cd launcher
npm install
npm run dev
```

- [ ] Window opens (Login screen after a brief boot state).
- [ ] "Sign in with Microsoft" → real login works → username in the sidebar.
- [ ] Close and reopen (`npm run dev`) → still signed in (silent restore).
- [ ] Settings: drag the RAM slider, restart → value persisted. "Open log folder" works.
- [ ] Home: version list shows "Minecraft 1.21.11" (fetched from the repo manifest — requires step 0 public).

## 3. The big one: Play (closes M2 — roadmap P2-13)

- [ ] Press **Play**. Expected stage order: Preparing → Setting up Java (Temurin 21 download, first time only) → Installing mods (Fabric API, Sodium, Lithium from Modrinth + the Axo client jar from the GitHub release) → Downloading game → Launching → Minecraft opens with Axo modules active.
- [ ] Quit the game — launcher returns to idle; `logs/game-*.log` exists.
- [ ] Press Play again — warm start, no re-downloads, much faster.
- [ ] Settings → "Repair installation" → reports files intact.

## 4. Login edge cases (roadmap P2-08, do what you can)

- [ ] Cancel the Microsoft popup mid-login → readable error, app usable.
- [ ] Sign out → sign in again.

## Reporting back

For any failure: the on-screen message, plus `%APPDATA%/axo-launcher/logs/launcher.log` (launcher issues) or `logs/game-*.log` / `client/run` logs (game issues). Milestones M1 + M2 are done when sections 1 and 3 pass.
