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

> Version pins in `client/gradle.properties` (Loom, Fabric Loader) were written without network access to Fabric's servers — verify them against https://fabricmc.net/develop before the first build (roadmap task P1-01).

### Launcher (`launcher/`)

Requires Node 20+.

```bash
cd launcher
npm install
npm run dev        # dev window with hot reload
npm run typecheck  # strict TS check
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

Bootstrap phase. The scaffolds build the skeleton described in the architecture doc; features land by executing roadmap tasks in order.
