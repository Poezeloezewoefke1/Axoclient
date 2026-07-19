# Axo Client — MVP Plan

The MVP is **M3 on the roadmap**: a stranger visits the website, downloads the launcher, signs in with Microsoft, presses Play, and is in Minecraft 1.21.11 with Axo modules and bundled performance mods running. Nothing more.

## In the MVP

**Client (M1 scope)**
- Module system with persistent config
- HUD: FPS, CPS, coordinates, keystrokes
- QoL: fullbright, zoom
- In-game settings screen with toggles

**Launcher (M2 scope)**
- Microsoft login (msmc) with encrypted persistent session
- Manifest-driven install: Java 21, vanilla + Fabric Loader, bundled mods (Modrinth, sha1-verified), Axo client jar
- Launch with progress UI, basic settings (RAM, install dir), readable errors + logs

**Distribution (M3 scope)**
- Static download page with latest-release button
- CI builds on tags for both artifacts; manifest published with the website
- One real release (v0.1.0) verified on a clean Windows VM

## Explicitly NOT in the MVP

| Cut | Why | Returns in |
|---|---|---|
| Cosmetics, capes, accounts backend | needs servers + moderation; zero servers is an MVP invariant | maybe never — decide post-MVP |
| Launcher self-update | ships days after MVP via electron-updater | Phase 3 |
| macOS / Linux | Windows-first per project constraints | post-M4 decision |
| Multiple Minecraft versions | design supports it; effort doesn't yet | Phase 6 |
| HUD drag-editing, onboarding, offline mode | polish, not proof | Phase 5 |
| Code signing | costs money; SmartScreen warning is tolerable for first users | Phase 5 decision |
| Changelog/FAQ pages | download-only website scope | when content exists |

## MVP acceptance test (run verbatim, clean Windows VM)

1. Open the website → click Download → installer runs.
2. Launch Axo → Microsoft login completes → profile name visible.
3. Press Play → progress reaches "Launching" → Minecraft 1.21.11 title screen.
4. Join a world → toggle FPS HUD + zoom via settings screen → both work.
5. Quit → relaunch → still logged in → Play works again in under 30 s (warm start, no re-downloads).

All five pass = MVP done. Anything else found on the way becomes a filed task, not scope creep.
