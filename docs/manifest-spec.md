# Axo Manifest Specification (v1)

> **Status: FROZEN v1 (2026-07-19, P0-04).** Changes within schemaVersion 1 must be additive (new optional fields only) and must update both validators in the same PR: `manifest/validate.mjs` and `launcher/src/main/manifest.ts`.

`axo-manifest.json` is the contract between the launcher, the release pipeline, and the website. The launcher must treat it as the **only** source of truth about playable versions. Canonical URL once the website is deployed: `https://<site-domain>/manifest/axo-manifest.json`. Until then the launcher points at the repo raw URL (see `launcher/src/main/manifest.ts`).

Versioning rule: **additive changes only** within `schemaVersion: 1` (new optional fields are fine). Breaking changes bump `schemaVersion`, and launchers refuse manifests newer than they understand while telling the user to update.

## Top level

| Field | Type | Req | Meaning |
|---|---|---|---|
| `schemaVersion` | int | ✔ | Currently `1`. |
| `generatedAt` | ISO-8601 string | ✔ | When this manifest was published. |
| `launcher` | object | ✔ | Launcher self-update policy. |
| `channels` | object | ✔ | Map of channel id (`stable`, `beta`) → Channel. |

## `launcher`

| Field | Type | Req | Meaning |
|---|---|---|---|
| `minimumVersion` | semver string | ✔ | Launchers older than this must self-update before installing anything. |
| `releasesRepo` | string | ✔ | `owner/repo` whose GitHub Releases feed electron-updater watches. |

## Channel

| Field | Type | Req | Meaning |
|---|---|---|---|
| `default` | string | ✔ | Version `id` selected for new users. |
| `versions` | Version[] | ✔ | Newest first. |

## Version

| Field | Type | Req | Meaning |
|---|---|---|---|
| `id` | string | ✔ | Axo version id, e.g. `"1.21.11-r1"`. Unique across channels. |
| `mcVersion` | string | ✔ | Exact Minecraft version, e.g. `"1.21.11"`. |
| `fabricLoaderVersion` | string | ✔ | Exact Fabric Loader version to install. |
| `javaMajor` | int | ✔ | Required Java feature version (21 for 1.21.11). |
| `client` | Artifact | ✔ | The Axo client mod jar itself. |
| `mods` | Mod[] | ✔ | Bundled mods, installed alongside the client jar. |
| `notes` | string | ✗ | One-line changelog shown in the launcher. |

## Artifact

| Field | Type | Req | Meaning |
|---|---|---|---|
| `version` | string | ✔ | Artifact's own version, e.g. `"0.1.0"`. |
| `url` | https URL | ✔ | Absolute download URL (GitHub Release asset). |
| `sha1` | hex string | ✔ | Verified after download; mismatch = hard fail + re-download. |
| `size` | int | ✗ | Bytes, for progress bars. |

## Mod

Same fields as Artifact, plus:

| Field | Type | Req | Meaning |
|---|---|---|---|
| `id` | string | ✔ | Stable slug, e.g. `"sodium"`. Used as the on-disk filename key. |
| `source` | `"modrinth"` | ✔ | Only Modrinth in v1. Others may be added (additive). |
| `modrinthProject` | string | ✔ | Modrinth project id/slug. |
| `modrinthVersion` | string | ✔ | Modrinth version id — pins the exact file; `url` is that file's CDN URL. |
| `required` | bool | ✗ (default `true`) | Optional mods can be toggled in launcher settings later. |

**Licensing rule encoded here:** mods are always fetched from Modrinth's CDN at install time by the user's launcher. We never rehost or bundle third-party jars in our releases.

## Launcher behavior requirements

1. Validate with zod before use; on failure, fall back to last cached valid manifest and surface a warning.
2. Cache the last valid manifest on disk (offline launches of already-installed versions must work).
3. Sync semantics for `mods/`: download anything missing/hash-mismatched, delete jars not in the list (the game dir is launcher-owned; users don't hand-manage it).
4. Never interpolate URLs — use them exactly as given.

## Worked example

See [`../manifest/axo-manifest.json`](../manifest/axo-manifest.json). Placeholder `sha1`/`url` values (all-zero hashes) are used until the first real release is published (task P4-07); the manifest validation script (task P3-08) rejects placeholders in CI once releases exist.

## Adding a new Minecraft version later (the whole point)

1. Port the client mod (see `porting-checklist.md`, written in Phase 6).
2. Publish the new client jar as a GitHub Release asset.
3. Pick bundled mod versions for the new MC version on Modrinth.
4. Append a Version entry to `beta`, test, promote to `stable`.

No launcher code changes. That's the design invariant to protect in code review.
