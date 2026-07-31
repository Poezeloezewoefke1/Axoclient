# Axo Client

A custom Minecraft Java Edition client — Fabric-based client mod, Windows desktop launcher with Microsoft login, and a public download website. Clean and professional, light blue on black.

**First supported version: Minecraft 1.21.11 (Fabric).** Everything is designed data-driven so new Minecraft versions are added by editing a manifest, not rewriting code.

## Repository map

| Path | What it is | Stack |
|---|---|---|
| [`docs/`](docs/) | Full planning package: architecture, roadmap (60+ tasks), MVP plan, risks, decisions | Markdown |
| [`client/`](client/) | The Axo Client Fabric mod — module system, HUD, built-in features | Java 21, Gradle + Fabric Loom |
| [`launcher/`](launcher/) | Windows desktop launcher — Microsoft login, install, launch, update | Electron + TypeScript + React |
| [`website/`](website/) | Public download page | Static HTML/CSS/JS |
| [`manifest/`](manifest/) | `axo-manifest.json` — the version manifest all systems read | JSON (spec in [`docs/manifest-spec.md`](docs/manifest-spec.md)) |

## Start here

1. Read [`docs/architecture.md`](docs/architecture.md) — how the three systems connect.
2. Read [`docs/mvp.md`](docs/mvp.md) — what "done enough to ship" means.
3. Work through [`docs/roadmap.md`](docs/roadmap.md) — 60+ small tasks in dependency order. Each task is sized 15 min – 2 h and has explicit done-criteria, so any task can be handed to a person or an AI in isolation.
4. Check [`docs/risks.md`](docs/risks.md) before investing heavily — one item (Microsoft/Mojang API approval) should be started **today** because it has external lead time.

## Quickstart per app

### Client mod (`client/`)

Requires JDK 21.

```bash
cd client
./gradlew build      # produces build/libs/axoclient-<version>.jar
./gradlew runClient  # launches a dev instance of Minecraft 1.21.11
```

In-game controls and the full feature list live in
[`docs/client-features.md`](docs/client-features.md) — start with **Right
Shift** to open the ClickGUI.

> The version pins in `client/gradle.properties` (Loom, Fabric Loader, Fabric API) are verified on every push — the **Client build** CI job resolves the whole toolchain against Fabric's real servers, so a stale pin turns the build red. The same job prints the current versions for 1.21.11, so when you do need to bump them the values are in the log.

### Launcher (`launcher/`)

Requires Node 20+.

```bash
cd launcher
npm install
npm run dev        # dev window with hot reload
npm run typecheck  # strict TS check
npm test           # unit tests (vitest)
npm run build      # production bundle
npm run dist       # Windows NSIS installer (run on Windows)
```

### Website (`website/`)

No build step. Open `website/index.html` in a browser, or serve it:

```bash
cd website
npx serve .
```

## Status

Well past scaffold. The client ships ~78 modules (HUD, combat, quality-of-life, cosmetics) with an in-game ClickGUI, a drag-to-place HUD editor and custom menu artwork; the launcher does multi-account Microsoft login, hash-verified installs, skins, mods, screenshots, profiles, saved servers, crash diagnosis and self-update. Completed roadmap tasks carry a **Status** line.

Remaining big-ticket items: porting the mod to further Minecraft versions, a code-signed installer, and macOS/Linux builds.

CI (`.github/workflows/ci.yml`) runs on every push: manifest validation (`node manifest/validate.mjs`, strict), launcher typecheck + tests + build, and the client Gradle build — which doubles as toolchain-pin verification. Tagged releases are built by `client-release.yml` / `launcher-release.yml`; both refuse a tag that disagrees with the version file. See [`docs/releasing.md`](docs/releasing.md).

## Reference

| Doc | When you need it |
|---|---|
| [`docs/architecture.md`](docs/architecture.md) | How the three systems connect, and the rules each follows |
| [`docs/client-features.md`](docs/client-features.md) | Every module, the in-game controls, how to change the menu artwork |
| [`docs/adding-a-version.md`](docs/adding-a-version.md) | Supporting a new Minecraft version, step by step |
| [`docs/releasing.md`](docs/releasing.md) | Cutting a release; producing the `.exe` |
| [`docs/discord-presence.md`](docs/discord-presence.md) | Turning on "Playing Axo Client" on Discord |
| [`docs/crash-handling.md`](docs/crash-handling.md) | How crashes are caught and explained to players |

## Community

[Discord](https://discord.gg/nKXpBaeeyy) · [Issues](https://github.com/Poezeloezewoefke1/Axoclient/issues)
