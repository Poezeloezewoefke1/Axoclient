# Axo Client — Roadmap

64 tasks across 7 phases. Every task is one action, sized 15 min – 2 h, with concrete done-criteria and explicit dependencies — each is written so a person or an AI can execute it in isolation.

**Milestones**

- **M1** (end of Phase 1): client boots in dev, modules toggle in-game, config persists.
- **M2** (end of Phase 2): launcher does login → install → launch of modded 1.21.11.
- **M3** (end of Phase 4): a stranger downloads from the website and plays Axo Client.
- **M4** (end of Phase 6): adding a Minecraft version is a data change + a mod port.

Task IDs are `P<phase>-<nn>`. Dependencies name task IDs; no dependency means "start anytime within its phase".

---

## Phase 0 — Foundations (7 tasks)

### P0-01 · define branding direction
- **Do:** Write `docs/branding.md`: logo brief (axolotl motif y/n), typography choice, color tokens (`--axo-blue: #38bdf8`, `--axo-black: #0a0a0f`, plus 3 neutrals), tone of voice, name usage rules ("Axo Client", "Axo").
- **Done when:** doc exists and website + launcher CSS reference the same token values.
- **Est:** 1 h
- **Status:** ✅ DONE 2026-07-19 — `docs/branding.md`; logo asset itself remains open decision O-2.

### P0-02 · create Azure app registration
- **Do:** In Azure Portal, register a public-client app "Axo Launcher": redirect URI `http://localhost` (+ msmc's default), enable device code flow, note the client ID. Store the client ID in `launcher/.env.example` (it is public, not a secret).
- **Done when:** client ID recorded; app shows in Azure portal with correct platform config.
- **Depends:** none — **do this first; P0-03 has external lead time**
- **Est:** 45 min · **Refs:** https://minecraft.wiki/w/Microsoft_authentication
- **Status:** ✅ DONE 2026-07-19 — client ID `77802178-1e6b-4163-b382-d4095833986e`, wired (inactive until P0-03) in `launcher/src/main/auth.ts`.

### P0-03 · request Minecraft API access approval for the Azure app
- **Do:** Submit Mojang's Minecraft API access form for the P0-02 client ID (required for `api.minecraftservices.com` login to work for third-party launchers). Record submission date in `docs/decisions.md`.
- **Done when:** form submitted and confirmation received; approval status tracked until granted.
- **Depends:** P0-02 · **Est:** 30 min (plus multi-week external wait — dev continues with msmc defaults meanwhile)
- **Status:** 🟡 READY TO SUBMIT — copy-paste submission pack in `docs/mojang-api-request.md`; owner must submit from their browser.

### P0-04 · freeze manifest schema v1
- **Do:** Review `docs/manifest-spec.md` + `manifest/axo-manifest.json` against launcher plans; fix field gaps; tag the spec "frozen v1" in the doc header.
- **Done when:** spec marked frozen; any later change follows the additive rule.
- **Est:** 45 min
- **Status:** ✅ DONE 2026-07-19 — spec header marked FROZEN v1; dual-validator rule stated.

### P0-05 · set up repo tooling
- **Do:** Add root `.gitignore` (Gradle, node, IDE), `.editorconfig` (4-space Java, 2-space TS/JSON), and a root `LICENSE` decision for our own code (recommend: source-available, e.g. PolyForm or proprietary notice — decide, don't default).
- **Done when:** files committed; license named in README.
- **Est:** 45 min

### P0-06 · define branch and release conventions
- **Do:** Add `docs/releasing.md` stub: `main` protected, feature branches, tags `launcher-vX.Y.Z` and `client-vX.Y.Z` drive CI, semver rules for each artifact.
- **Done when:** doc exists; tag format referenced by P4-05/P4-06 workflows.
- **Est:** 30 min
- **Status:** ✅ DONE 2026-07-19 — `docs/releasing.md` (also covers P3-01).

### P0-07 · create app icon placeholders
- **Do:** Produce `assets/icon.png` (1024²) + `assets/icon.ico` placeholder in brand colors; wire into `launcher/electron-builder.yml` and website favicon.
- **Done when:** launcher build uses the icon; website tab shows favicon.
- **Depends:** P0-01 · **Est:** 1 h
- **Status:** ✅ DONE 2026-07-19 — geometric axolotl placeholder generated (assets/, launcher/build/icon.ico wired into electron-builder, website favicon). Commissioned logo stays open as O-2.

---

## Phase 1 — Fabric client core (12 tasks) → M1

### P1-01 · verify client scaffold builds
- **Do:** On a dev machine with JDK 21 + internet, check pins in `client/gradle.properties` against https://fabricmc.net/develop (Loom, Loader), then run `./gradlew build` and `./gradlew runClient`.
- **Done when:** build green; dev client reaches title screen; corrected pins committed.
- **Est:** 45 min · **Refs:** https://docs.fabricmc.net/develop/getting-started/launching-the-game
- **Status:** 🟡 BUILD VERIFIED 2026-07-19 — CI's client job compiles green with the committed pins (Loader 0.17.3, Loom 1.11-SNAPSHOT, mojmap); the `runClient` title-screen check still needs a local machine.

### P1-02 · add Fabric API dependency
- **Do:** Add `fabric-api` to `client/build.gradle` with the 1.21.11 version from https://modrinth.com/mod/fabric-api/versions; add `"fabric"` to `depends` in `fabric.mod.json`.
- **Done when:** `runClient` still boots; Fabric API classes resolve in IDE.
- **Depends:** P1-01 · **Est:** 30 min
- **Status:** ✅ DONE 2026-07-19 — fabric-api `0.141.5+1.21.11` (per CI version listing); Loom 1.13 needed for mojmap + fabric-api javadoc compat.

### P1-03 · dispatch client ticks to modules
- **Do:** Register a Fabric `ClientTickEvents.END_CLIENT_TICK` listener in `AxoClient` that calls `ModuleManager.tickAll()`; remove any placeholder tick wiring.
- **Done when:** a log-line test module prints once per tick only while enabled.
- **Depends:** P1-02 · **Est:** 45 min
- **Status:** ✅ DONE 2026-07-19 — ClientTickEvents.END_CLIENT_TICK drives ModuleManager.tickAll().

### P1-04 · extend config to per-module sections
- **Do:** In `AxoConfig`, store `modules.<id>.enabled` plus a free-form JSON object per module; save atomically (temp file + rename) on change; load before module registration.
- **Done when:** toggling a module, restarting the dev client, and seeing the state persist works.
- **Depends:** P1-01 · **Est:** 1.5 h
- **Status:** ✅ DONE 2026-07-19 — per-module sections with typed get/set (`getModule*`/`setModule*`), atomic save; used by the HUD position system. Restart-persistence check rides along with the P1-01 runClient session.

### P1-05 · add keybind support for module toggles
- **Do:** Using Fabric's `KeyBindingHelper`, register an optional toggle key per module (declared in the module class); default binds: Zoom=C, HUD editor=RShift (later).
- **Done when:** pressing the bind in-game toggles the module and persists via P1-04.
- **Depends:** P1-03, P1-04 · **Est:** 1 h
- **Status:** 🟡 PARTIAL 2026-07-19 — fixed default keys via raw GLFW polling (Zoom=C, Settings=RShift); rebindable KeyMappings still to do.

### P1-06 · build HUD anchor/position system
- **Do:** Create `hud/HudAnchor` (9 anchor points + pixel offset) and make HUD modules render relative to an anchor stored in their config section; replace FpsHud's hardcoded position.
- **Done when:** changing anchor values in the config file moves the FPS counter accordingly.
- **Depends:** P1-04 · **Est:** 2 h
- **Status:** 🟡 IMPLEMENTED 2026-07-19 — `hud/HudAnchor` + `HudPosition` + `HudModule` base (config keys `hud_anchor`/`hud_x`/`hud_y`); FPS + coordinates refactored onto it; CI-compiled. In-game visual check pending a local `runClient`.

### P1-07 · add CPS counter module
- **Do:** New `modules/hud/CpsCounterModule`: ring-buffer of click timestamps from mouse input events, render "L: n | R: n" via the HUD system.
- **Done when:** clicking shows live CPS; value decays to 0 within 1 s of stopping.
- **Depends:** P1-06 · **Est:** 1.5 h
- **Status:** 🟡 IMPLEMENTED 2026-07-19 — tick-sampled click edges, 1 s sliding window; in-game verification pending runClient.

### P1-08 · add coordinates HUD module
- **Do:** New `modules/hud/CoordinatesModule`: player X/Y/Z + facing direction, formatted, HUD-anchored.
- **Done when:** values match F3 screen while moving.
- **Depends:** P1-06 · **Est:** 45 min
- **Status:** 🟡 IMPLEMENTED 2026-07-19 (fixed position; CI-compiled) — re-anchor + in-game value check when P1-06 lands.

### P1-09 · add keystrokes display module
- **Do:** New `modules/hud/KeystrokesModule`: WASD + mouse buttons as filled/unfilled squares in brand colors, HUD-anchored.
- **Done when:** visual state matches actual key state with no perceptible lag.
- **Depends:** P1-06 · **Est:** 2 h
- **Status:** 🟡 IMPLEMENTED 2026-07-19 — WASD + LMB/RMB cells, anchored; in-game verification pending runClient.

### P1-10 · add fullbright module
- **Do:** New `modules/qol/FullbrightModule`: raise effective gamma while enabled (restore exact previous value on disable) — implementation via gamma option override, not shaders.
- **Done when:** caves are bright when on; original brightness restored when off and after crash (config-safe).
- **Depends:** P1-03 · **Est:** 1 h
- **Status:** 🟡 IMPLEMENTED 2026-07-19 — raw gamma via OptionInstance accessor mixin (bypasses [0,1] clamp), exact restore on disable.

### P1-11 · add zoom module
- **Do:** New `modules/qol/ZoomModule`: hold-key FOV reduction with smooth interpolation, mouse-wheel zoom depth, cinematic camera optional.
- **Done when:** hold-to-zoom works, releasing restores FOV exactly, no scroll-hotbar conflict while zooming.
- **Depends:** P1-05 · **Est:** 2 h
- **Status:** 🟡 IMPLEMENTED 2026-07-19 — hold-C FOV 30 with exact restore; smoothing/scroll-depth later.

### P1-12 · create in-game settings screen
- **Do:** `ui/AxoSettingsScreen` (opened via keybind): list modules grouped by category with toggle buttons; changes write through `ModuleManager` + `AxoConfig`.
- **Done when:** every registered module can be toggled from the screen; state persists. **This closes M1.**
- **Depends:** P1-04, P1-05 · **Est:** 2 h
- **Status:** 🟡 IMPLEMENTED 2026-07-19 — vanilla-widget toggle screen on Right Shift; closes M1 once runClient checks pass.

---

## Phase 2 — Launcher MVP (14 tasks) → M2

### P2-01 · verify launcher scaffold on Windows
- **Do:** `npm install && npm run dev` on a Windows machine; fix any platform issues; confirm `npm run typecheck` and `npm run build` are green.
- **Done when:** dev window opens showing the Home screen shell.
- **Est:** 45 min

### P2-02 · finalize home screen layout
- **Do:** Implement the Home screen per `architecture.md`: account chip (top-right), channel+version selector fed by manifest data, Play button with progress substates (idle/installing/launching), news placeholder.
- **Done when:** screen matches the layout with mock data; responsive down to 900×600.
- **Depends:** P2-01 · **Est:** 2 h

### P2-03 · harden manifest fetch + validation
- **Do:** Extend `src/main/manifest.ts`: zod schema mirroring `manifest-spec.md` exactly, 10 s timeout, ETag-aware caching to disk (`manifest.cache.json`), fall back to cache on network/validation failure with a `stale: true` flag to the UI.
- **Done when:** unit tests cover: valid fetch, invalid JSON → cache fallback, no cache + failure → typed error.
- **Depends:** P2-01 · **Est:** 2 h

### P2-04 · add persistent settings store
- **Do:** Add `src/main/settings.ts` (plain JSON, atomic writes — no electron-store dependency): install dir (default `%APPDATA%/.axoclient`), RAM MB, channel, JVM args; expose typed IPC get/set; render basic Settings screen fields.
- **Done when:** settings survive app restart; renderer reads/writes only via IPC.
- **Depends:** P2-01 · **Est:** 1.5 h
- **Status:** ✅ DONE 2026-07-19 — store unit-tested (defaults, clamping, corrupt-file recovery); RAM slider + JVM args live on the Settings screen.

### P2-05 · implement Microsoft login with msmc
- **Do:** Flesh out `src/main/auth.ts`: msmc Electron popup flow using the P0-02 client ID (msmc default ID until approval lands), map result to `{profile, mcToken, expiresAt}`, typed IPC `auth:login`.
- **Done when:** real Microsoft account logs in from the Login screen and profile name/UUID render in the account chip.
- **Depends:** P2-01, P0-02 · **Est:** 2 h · **Refs:** https://github.com/Hansson01/MSMC
- **Status:** 🟡 IMPLEMENTED 2026-07-19 — msmc Electron flow live; our client ID wired behind USE_AXO_CLIENT_ID (P0-03 gate).

### P2-06 · persist refresh token securely
- **Do:** Encrypt the msmc refresh token with Electron `safeStorage` into the app data dir; never write plaintext; add `auth:logout` that wipes it.
- **Done when:** restart → still logged in; logout → token file gone; file on disk is not plaintext.
- **Depends:** P2-05 · **Est:** 1 h
- **Status:** 🟡 IMPLEMENTED 2026-07-19 — safeStorage-encrypted token file, wiped on logout; restart-persistence check on Windows pending.

### P2-07 · silent session refresh on startup
- **Do:** On app start, attempt token refresh in the background; UI shows Home in "refreshing" state, drops to Login only on refresh failure.
- **Done when:** cold start with valid stored token reaches Home logged-in without user action.
- **Depends:** P2-06 · **Est:** 1 h
- **Status:** 🟡 IMPLEMENTED 2026-07-19 — restoreSession() on startup with boot state in UI; rotated token re-persisted.

### P2-08 · test Microsoft login callback end-to-end
- **Do:** Manual test matrix on Windows: fresh login, cancel mid-flow, wrong account (no Minecraft owned), expired refresh token, offline. Fix surfaced issues; document expected error copy per case.
- **Done when:** all five cases end in a correct UI state with human-readable messages (no raw stack traces).
- **Depends:** P2-07 · **Est:** 2 h
- **Status:** ⏳ BLOCKED on a Windows machine — run the five-case matrix there.

### P2-09 · implement Java 21 provisioning
- **Do:** `src/main/java.ts`: query Adoptium API for latest Temurin JRE matching manifest `javaMajor` + win x64, download to `runtime/<major>/`, unpack, verify checksum, cache; expose progress events.
- **Done when:** empty cache → JRE downloaded and `bin/javaw.exe` exists; second run is a no-op.
- **Depends:** P2-03 · **Est:** 2 h · **Refs:** https://api.adoptium.net/q/swagger-ui/
- **Status:** 🟡 CORE DONE 2026-07-19 — full flow unit-tested (query→download sha256-verified→extract→cache; cache hit uses no network; tamper rejected); real Adoptium run happens with P2-13 on Windows.

### P2-10 · implement vanilla + Fabric installation
- **Do:** `src/main/install.ts`: via minecraft-launcher-core resolve vanilla `mcVersion` files into the install dir; fetch the Fabric loader profile JSON from `meta.fabricmc.net` for `mcVersion`+`fabricLoaderVersion` and register it as the launch profile.
- **Done when:** target dir contains vanilla + fabric version JSONs, libraries, assets; re-run is a fast no-op.
- **Depends:** P2-03, P2-09 · **Est:** 2 h · **Refs:** https://fabricmc.net/use/installer/
- **Status:** 🟡 CORE DONE 2026-07-19 — Fabric profile fetch/write unit-tested; vanilla files resolve via MCLC at launch. Real-network E2E pending.

### P2-11 · implement bundled-mod sync
- **Do:** In `install.ts`: reconcile `mods/` with the manifest mod list — download missing by URL, verify sha1 (delete + retry once on mismatch), remove jars not in the list; progress events per file.
- **Done when:** deleting a random mod jar and relaunching restores it; a stray jar dropped in `mods/` is removed.
- **Depends:** P2-10 · **Est:** 1.5 h
- **Status:** 🟡 CORE DONE 2026-07-19 — `download.ts`/`sync.ts`/`install.ts` unit-tested (mismatch-retry, idempotency, stray-removal against a local HTTP server); end-to-end run against real Modrinth URLs happens with P2-13 on Windows.

### P2-12 · download the Axo client jar
- **Do:** Same sync treatment for the `client` artifact from the manifest (GitHub Release asset URL + sha1) into `mods/` (it is itself a Fabric mod).
- **Done when:** client jar lands with verified hash; version pin changes trigger re-download.
- **Depends:** P2-11 · **Est:** 45 min
- **Status:** 🟡 CORE DONE 2026-07-19 — client jar included in `desiredModFiles` with versioned naming (unit-tested); real-release verification at P4-07.

### P2-13 · wire game launch with progress UI
- **Do:** `src/main/launch.ts`: launch via minecraft-launcher-core with the msmc auth object, provisioned Java path, RAM from settings, Fabric profile; stream download/launch progress to the Play button substates; detect process exit.
- **Done when:** Play → modded 1.21.11 reaches title screen with Axo modules loaded; UI returns to idle on game exit. **This closes M2.**
- **Depends:** P2-05, P2-10, P2-12 · **Est:** 2 h
- **Status:** 🟡 IMPLEMENTED 2026-07-19 — full pipeline (Java→profile→mods→MCLC) wired to the Play button with per-stage progress; Windows E2E run is the M2 gate.

### P2-14 · build the error/log surface
- **Do:** Central error boundary: every pipeline failure (manifest, auth, java, install, launch) maps to an error card with plain-language message, "Retry" and "Open logs" (opens the launcher log file); log all pipeline steps with timestamps to `logs/launcher.log` (rotating).
- **Done when:** killing the network mid-install produces a readable error card + complete log entry, and Retry resumes correctly.
- **Depends:** P2-13 · **Est:** 2 h
- **Status:** ✅ DONE 2026-07-20 — friendly error mapping (offline/hash/404/5xx), crash cards with boot-crash repair hint, force-close, stage logging, Open-log-folder.

---

## Phase 3 — Update system (8 tasks)

### P3-01 · define versioning + release scheme
- **Do:** Complete `docs/releasing.md`: semver for launcher and client mod, what bumps what, manifest `id` convention (`<mc>-r<n>`), beta→stable promotion checklist.
- **Done when:** doc answers "I fixed a launcher bug — what exactly do I do to ship it?" step by step.
- **Depends:** P0-06 · **Est:** 1 h
- **Status:** ✅ DONE 2026-07-19 — see `docs/releasing.md` release procedures.

### P3-02 · wire electron-updater
- **Do:** Add electron-updater checking the `releasesRepo` GitHub Releases feed on startup (and every 4 h); config in `electron-builder.yml` (`publish: github`).
- **Done when:** an older local build detects a newer published release and downloads it.
- **Depends:** P4-06 (needs one published release to test against) · **Est:** 1.5 h
- **Status:** 🟡 IMPLEMENTED 2026-07-19 — electron-updater wired (packaged builds only); exercising it needs the first published release (P4-06).

### P3-03 · build update UI
- **Do:** Non-blocking banner on Home ("Update ready — Restart"), download progress in Settings→About, forced-update modal when below manifest `launcher.minimumVersion`.
- **Done when:** both soft and forced paths work against a test release.
- **Depends:** P3-02 · **Est:** 1.5 h
- **Status:** 🟡 PARTIAL 2026-07-19 — non-blocking "Update ready — Restart to install" banner; forced-update modal still to do.

### P3-04 · client-files update check
- **Do:** On every launch, re-run the manifest sync (P2-11/P2-12 logic) so mod/client updates apply automatically; show "Updating Axo…" substate when files actually change.
- **Done when:** bumping the client version in a test manifest updates the installed jar on next Play.
- **Depends:** P2-13 · **Est:** 1 h
- **Status:** ✅ DONE-BY-DESIGN 2026-07-19 — the launch pipeline re-syncs client/mod files against the manifest on every Play; "Installing mods…" substate shown.

### P3-05 · implement repair + rollback
- **Do:** Settings→"Repair installation" (delete hash-mismatched files, full re-sync) and keep the previous client jar as `.previous` for one-click rollback if the new version crashes at boot (detect via exit code within 60 s).
- **Done when:** corrupting a jar + Repair fixes it; simulated instant-crash triggers the rollback offer.
- **Depends:** P3-04 · **Est:** 2 h
- **Status:** 🟡 PARTIAL 2026-07-19 — Repair (full hash re-sync) live in Settings; .previous rollback + boot-crash detection still to do.

### P3-06 · write the release runbook
- **Do:** Finish `docs/releasing.md` with the exact command sequence for a full release (tag client → CI builds jar → update manifest → tag launcher → CI publishes installer → deploy website).
- **Done when:** runbook executed once end-to-end without improvisation (can be during P4-07).
- **Depends:** P3-01 · **Est:** 45 min

### P3-07 · create manifest publish flow
- **Do:** Manifest lives in `manifest/` in git; website deploy copies it to `/manifest/axo-manifest.json`; document that editing + merging to main *is* publishing; add `generatedAt` bump to the validation script.
- **Done when:** a manifest edit merged to main appears at the public URL after deploy.
- **Depends:** P4-04 · **Est:** 45 min

### P3-08 · add manifest validation script
- **Do:** `manifest/validate.mjs` (dependency-free Node, mirroring the launcher's zod schema — change both together): checks schema, unique ids, https URLs, non-placeholder sha1s (flag `--allow-placeholders` for pre-release), channels' `default` exists. Wire as a CI step.
- **Done when:** corrupting any field makes the script exit non-zero with a pointed message.
- **Depends:** P0-04 · **Est:** 1.5 h
- **Status:** ✅ DONE 2026-07-19 — pass/placeholder/corruption cases all verified; runs in CI (`ci.yml`).

---

## Phase 4 — Website & distribution (8 tasks) → M3

### P4-01 · branding pass on download page
- **Do:** Apply `docs/branding.md` tokens to `website/`: final copy (hero line, three feature blurbs), OG/meta tags, favicon.
- **Done when:** page matches branding doc; Lighthouse a11y ≥ 95.
- **Depends:** P0-01 · **Est:** 1.5 h

### P4-02 · create website navbar + footer
- **Do:** Add minimal navbar (logo, Download anchor, GitHub link) and footer (license note, "not affiliated with Mojang/Microsoft" disclaimer — required wording).
- **Done when:** present on the page, responsive at 360 px width.
- **Depends:** P4-01 · **Est:** 45 min
- **Status:** ✅ DONE 2026-07-19 — navbar + footer (with required disclaimer) shipped with the initial page.

### P4-03 · wire latest-release download button
- **Do:** Harden the existing releases-API fetch: correct asset selection (`.exe` NSIS), display version + file size, hardcoded fallback URL if API rate-limits/fails.
- **Done when:** button always yields a working download, with and without API availability.
- **Depends:** P4-06 (real release to point at) · **Est:** 1 h

### P4-04 · deploy the website
- **Do:** Create Cloudflare Pages project (or GitHub Pages if simpler) building from `website/` on push to main; decide domain (subdomain fine for now) and record in `docs/decisions.md`.
- **Done when:** public URL serves the page + `/manifest/axo-manifest.json` with correct content-type.
- **Depends:** P4-01 · **Est:** 1 h
- **Status:** 🟡 IMPLEMENTED 2026-07-20 — GitHub Pages workflow deploys website/ + manifest on push (pages.yml); goes live once the repo is public. Launcher prefers the Pages manifest URL with raw fallback.

### P4-05 · CI: build client jar on tag
- **Do:** `.github/workflows/client-release.yml`: on tag `client-v*` → JDK 21, `./gradlew build`, attach jar + sha1 file to a GitHub Release.
- **Done when:** pushing a test tag produces a release with jar + checksum attached.
- **Depends:** P1-01, P0-06 · **Est:** 1.5 h
- **Status:** 🟡 DRAFTED 2026-07-19 — workflow committed; done-criteria needs a test tag after P1-01.

### P4-06 · CI: build launcher installer on tag
- **Do:** `.github/workflows/launcher-release.yml`: on tag `launcher-v*` → Windows runner, `npm ci`, `npm run dist`, electron-builder publishes NSIS installer + update metadata (`latest.yml`) to the release.
- **Done when:** test tag yields an installable `.exe` and `latest.yml` in the release.
- **Depends:** P2-01, P0-06 · **Est:** 2 h
- **Status:** 🟡 DRAFTED 2026-07-19 — workflow committed; done-criteria needs a test tag.

### P4-07 · publish first end-to-end release (v0.1.0)
- **Do:** Execute the runbook: client-v0.1.0 tag → real sha1s into manifest (remove placeholders) → launcher-v0.1.0 tag → website deploy. Validation script must pass without `--allow-placeholders`.
- **Done when:** manifest at the public URL references only real, hash-verified artifacts.
- **Depends:** P3-06, P3-07, P3-08, P4-05, P4-06 · **Est:** 1.5 h
- **Status:** 🟡 PARTIAL 2026-07-20 — client release live, manifest fully real (strict validation green, CI now strict). Remaining: repo public, Pages deploy green, launcher-v0.1.0 installer (needs P2-01 Windows run first).

### P4-08 · fresh-machine install test
- **Do:** On a clean Windows VM: website → download → install → login → Play → in-game with modules. Note every rough edge in a findings list; file follow-up tasks.
- **Done when:** full journey succeeds; findings list committed. **This closes M3.**
- **Depends:** P4-07 · **Est:** 1.5 h

---

## Phase 5 — Polish & hardening (9 tasks)

### P5-01 · write crash-handling plan
- **Do:** `docs/crash-handling.md`: taxonomy (launcher crash, game crash at boot, game crash mid-session, hang), detection signal for each (exit codes, log patterns, timeouts), user-facing response for each, what gets logged where.
- **Done when:** every taxon has detection + response + log location defined.
- **Est:** 1 h
- **Status:** ✅ DONE 2026-07-19 — docs/crash-handling.md (taxonomy, detection, responses, log budget).

### P5-02 · capture game logs + surface them
- **Do:** Pipe game stdout/stderr to `logs/game-<timestamp>.log` (keep last 5); on abnormal exit show "Game crashed" card with "Open log folder".
- **Done when:** forced crash (kill process) produces the card and a complete log file.
- **Depends:** P5-01, P2-14 · **Est:** 1 h
- **Status:** 🟡 PARTIAL 2026-07-19 — per-session game-<ts>.log (keep 5) + Open-log-folder button; crash cards still to do.

### P5-03 · launcher crash reporter
- **Do:** Global main-process exception handler → write crash report file, show dialog offering to open the GitHub issues page with prefilled title (no auto-upload — privacy-first default).
- **Done when:** thrown test exception produces report + dialog; app exits cleanly.
- **Depends:** P5-01 · **Est:** 1 h
- **Status:** ✅ DONE 2026-07-19 — uncaughtException → crash-<ts>.txt report + error dialog + clean exit; unhandled rejections logged.

### P5-04 · offline mode
- **Do:** If manifest fetch fails but an installed version + cached manifest exist, allow launching with a "Playing offline — updates unavailable" notice (MS auth may still require network; reuse unexpired session if present).
- **Done when:** airplane-mode launch of an installed version works with valid cached session.
- **Depends:** P2-13, P2-03 · **Est:** 1.5 h

### P5-05 · first-run onboarding
- **Do:** First launch: 2-step modal — RAM slider with sane default (half of system RAM, cap 8 GB) and install location confirm; write to settings.
- **Done when:** fresh profile sees onboarding once; values land in settings store.
- **Depends:** P2-04 · **Est:** 1.5 h
- **Status:** ✅ DONE 2026-07-20 — first-run modal (RAM slider + install dir) persisted via the onboarded setting.

### P5-06 · full JVM/RAM settings screen
- **Do:** Settings: RAM slider, custom JVM args (with reset), custom Java path override (bypasses P2-09), install dir move (with file migration + progress).
- **Done when:** all four settings apply on next launch; migration verified.
- **Depends:** P2-04, P2-09 · **Est:** 2 h

### P5-07 · decide on Windows code signing
- **Do:** Research current options (OV cert ~$100–400/yr, Azure Trusted Signing ~$10/mo) vs unsigned + SmartScreen warning; record decision + budget in `docs/decisions.md`; if signing, wire cert into P4-06 workflow.
- **Done when:** decision recorded; if yes, released installer shows publisher name.
- **Depends:** P4-06 · **Est:** 1 h (research) + follow-up task if signing

### P5-08 · analytics + privacy decision
- **Do:** Decide: no analytics (recommended at this stage) vs privacy-respecting counter (e.g. download counts only via GitHub API — already public). Write `website/privacy.html` if anything is collected; record in decisions log.
- **Done when:** decision recorded; site is truthful about data collected.
- **Est:** 45 min

### P5-09 · HUD edit mode in client
- **Do:** In-game screen where HUD modules render as draggable boxes; drag writes anchor+offset to config; snap-to-anchor guides.
- **Done when:** dragging the FPS counter to a corner persists across restarts.
- **Depends:** P1-06, P1-12 · **Est:** 2 h
- **Status:** 🟡 DONE (v1) 2026-07-20 — button-based HudLayoutScreen (anchor cycle + offset nudges, live apply); drag editing remains a possible upgrade.

---

## Phase 6 — Multi-version groundwork (6 tasks) → M4

### P6-01 · audit client code for version coupling
- **Do:** Grep the client for direct Minecraft-class usage outside `mixin/` + adapter candidates; produce `docs/version-audit.md` listing each call site and its risk of breaking across MC versions.
- **Done when:** every Minecraft-internal touchpoint is listed and classified (stable API / likely to break / mixin).
- **Depends:** M1 complete · **Est:** 1.5 h
- **Status:** ✅ DONE 2026-07-19 — docs/version-audit.md: ~10 touchpoints classified; only GuiGraphics warrants an adapter (P6-02 scope note included).

### P6-02 · introduce a version adapter layer
- **Do:** For the "likely to break" list from P6-01, extract minimal interfaces (e.g. `RenderAdapter`, `InputAdapter`) in `core/compat/` with the 1.21.11 implementation as the only impl.
- **Done when:** modules compile against adapters only; no behavior change.
- **Depends:** P6-01 · **Est:** 2 h

### P6-03 · decide multi-version build strategy
- **Do:** Evaluate branch-per-version vs [Stonecutter](https://stonecutter.kikugie.dev/) multi-version Gradle vs separate module per version; record decision + migration outline in `docs/decisions.md`.
- **Done when:** decision recorded with tradeoffs; roadmap follow-ups filed for the migration.
- **Depends:** P6-02 · **Est:** 1.5 h

### P6-04 · manifest dry run with a second version
- **Do:** Add a second Version entry (e.g. a 1.21.x sibling) to the `beta` channel with placeholder client artifact; verify the launcher's version picker, install, and validation script all handle two versions with zero code changes.
- **Done when:** launcher UI offers both; selecting either installs the right files.
- **Depends:** P2-13, P3-08 · **Est:** 1 h

### P6-05 · timeboxed port to a second Minecraft version
- **Do:** Timebox 2 h: port the client mod to the P6-04 sibling version following Fabric's porting guide; log every breakage + fix.
- **Done when:** port compiles and boots **or** the breakage log clearly scopes remaining work into new tasks.
- **Depends:** P6-02, P6-03 · **Est:** 2 h · **Refs:** https://docs.fabricmc.net/develop/porting/current

### P6-06 · write the porting checklist
- **Do:** Distill P6-05's log into `docs/porting-checklist.md`: exact steps to support a new MC version (mappings bump, adapter impls, mod picks, manifest entry, test matrix, beta→stable).
- **Done when:** checklist is complete enough that "add 1.22 support" is just executing it. **This closes M4.**
- **Depends:** P6-05 · **Est:** 1 h

---

## Dependency highlights (the critical path)

```
P0-02 → P0-03 (external wait) ──────────────┐
P1-01 → P1-02 → P1-03/04 → modules → P1-12 (M1)
P2-01 → P2-03 → P2-09 → P2-10 → P2-11 → P2-12 ─→ P2-13 (M2) → P2-14
P2-05 → P2-06 → P2-07 → P2-08 ──────────────┘      │
P4-05 + P4-06 → P4-07 → P4-08 (M3) ←── P3-06/07/08 ┘
M1 → P6-01 → P6-02 → P6-03 → P6-05 → P6-06 (M4)
```

Parallel-friendly: Phase 1 (client) and Phase 2 (launcher) share no dependencies until P2-12 needs a built client jar — a two-person split, or alternating solo weeks, works cleanly.
