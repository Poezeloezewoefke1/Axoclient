# Axo Client — Decision Log

ADR-lite. One entry per decision; newest first. Status: ACCEPTED / OPEN / SUPERSEDED.

---

## D-008 · Client mod ships without Fabric API in the scaffold — ACCEPTED (2026-07-19)
The initial scaffold uses only Fabric Loader + one mixin (HUD render hook), so it has zero version-pinned mod dependencies. Fabric API is added deliberately in P1-02 when events/keybinds are wired.
**Tradeoff:** one extra early task vs a scaffold that can't fail on a stale Fabric API pin.

## D-007 · Mojang official mappings instead of Yarn — ACCEPTED (2026-07-19)
Loom's `officialMojangMappings()` needs no per-version mappings pin and matches most modern mod tooling.
**Tradeoff:** Mojang-named internals (fine); Yarn docs/snippets need mental translation. Revisit only if a critical dependency demands Yarn.

## D-006 · Bundled mods are fetched from Modrinth at install time — ACCEPTED (2026-07-19)
We never redistribute third-party jars (see risks R2). Manifest pins Modrinth project+version+sha1.
**Tradeoff:** install needs Modrinth availability; mitigated by sha1-verified caching and a future mirror-URL field.

## D-005 · Isolated game directory `%APPDATA%/.axoclient` — ACCEPTED (2026-07-19)
Axo never touches `.minecraft`. The launcher owns its directory completely (including deleting stray jars in `mods/`).
**Tradeoff:** duplicate assets on disk vs zero risk of corrupting the user's vanilla setup; asset reuse can be added later.

## D-004 · Zero-server backend: GitHub Releases + static site — ACCEPTED (2026-07-19, user choice)
Installers and jars on GitHub Releases; manifest + site static on Pages. No servers to run.
**Tradeoff:** no server-side features (accounts, cosmetics, telemetry) until a real backend is added behind the same URLs.

## D-003 · Launcher stack: Electron + TypeScript + React — ACCEPTED (2026-07-19, user choice)
Chosen over Compose Desktop/JavaFX/Tauri for UI polish and iteration speed. Auth via msmc, launching via minecraft-launcher-core, packaging via electron-builder, updates via electron-updater.
**Tradeoff:** ~100 MB installer and a second language ecosystem next to the Java mod; accepted knowingly.

## D-002 · First supported version: Minecraft 1.21.11 (Fabric), Java 21 — ACCEPTED (project constraint)
Single-version start; the manifest makes further versions additive.

## D-001 · Monorepo with client/ launcher/ website/ manifest/ docs/ — ACCEPTED (2026-07-19)
Solo-dev friendly: one clone, one issue tracker, atomic cross-cutting changes (e.g. manifest schema + launcher validator in one PR).
**Tradeoff:** CI must path-filter to avoid rebuilding everything on every push.

## D-009 · Manifest validator is dependency-free Node — ACCEPTED (2026-07-19)
`manifest/validate.mjs` uses no packages so CI and contributors validate with bare Node, no install step. The launcher's zod schema stays the runtime validator; spec changes must update both in one PR (noted in both files).
**Tradeoff:** two schema implementations to keep in sync vs a zero-dependency CI gate.

---

## OPEN decisions (owner input needed)

| ID | Decision | Blocking task | Notes |
|---|---|---|---|
| O-1 | License for our own code | P0-05 | recommend source-available (PolyForm Shield/Strict) or proprietary; MIT means forks can rebrand your client |
| O-2 | Final branding: logo, exact palette, typography | P0-01, P4-01 | axolotl motif is available and on-theme |
| O-3 | Domain name | P4-04 | subdomain (axo.pages.dev) is fine for MVP |
| O-4 | Code signing budget | P5-07 | ~$10/mo Azure Trusted Signing is the cheap path |
| O-5 | Analytics stance | P5-08 | recommendation: none for now |
| O-6 | Multi-version build strategy | P6-03 | Stonecutter vs branches — decide after M1 experience |
| O-7 | Azure app registration date + Mojang approval tracking | P0-02/03 | record submission + approval dates here |
