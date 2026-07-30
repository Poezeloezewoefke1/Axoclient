# Axo Client — Branches, Versions, and Releases (P0-06 + P3-01)

## Branch conventions

- `main` is protected: no direct pushes, PRs only, CI green required.
- Feature branches: `feat/<slug>`, fixes `fix/<slug>`, docs `docs/<slug>`.
- Roadmap work references its task ID in the PR title, e.g. `P2-05: Microsoft login via msmc`.

## Version scheme (semver per artifact)

Two independently versioned artifacts, released by pushing a tag:

| Artifact | Tag format | Version source | CI workflow |
|---|---|---|---|
| Client mod jar | `client-vX.Y.Z` | `client/gradle.properties` → `mod_version` | `.github/workflows/client-release.yml` |
| Launcher installer | `launcher-vX.Y.Z` | `launcher/package.json` → `version` | `.github/workflows/launcher-release.yml` |

Semver rules:

- **Patch** (Z): bug fixes, no behavior change users must know about.
- **Minor** (Y): new modules/screens/features; manifest additions.
- **Major** (X): breaking change — for the launcher, anything that requires a manifest `schemaVersion` bump or breaks old installs; for the client, config-format breaks (with migration).

The git tag must match the version file **exactly**; CI builds whatever the repo says, so bump the version file in the same PR that you intend to tag.

Axo *distribution* versions (what users pick in the launcher) are manifest `Version.id` entries: `<mcVersion>-r<n>` (e.g. `1.21.11-r2`), bumped whenever the client jar or the bundled-mod set changes for that Minecraft version.

## Release procedure

### Client mod

1. PR: bump `mod_version` in `client/gradle.properties` + changes. Merge to `main`.
2. Tag: `git tag client-vX.Y.Z && git push origin client-vX.Y.Z`.
3. CI builds the jar with JDK 21 and attaches `axoclient-X.Y.Z.jar` + `SHA1SUMS.txt` to a GitHub Release.
4. Update `manifest/axo-manifest.json`: new/updated Version entry pointing at the release asset URL with the **real sha1** from `SHA1SUMS.txt`; bump `generatedAt`.
5. `node manifest/validate.mjs` (no `--allow-placeholders`) must pass. PR + merge → manifest is live once the website deploys (P3-07/P4-04).

### Launcher

1. PR: bump `version` in `launcher/package.json` + changes. Merge to `main`.
2. Tag: `git tag launcher-vX.Y.Z && git push origin launcher-vX.Y.Z`.
3. CI (Windows runner) typechecks, builds, and publishes the NSIS installer **and `latest.yml`** to a GitHub Release — `latest.yml` is what electron-updater consumes (P3-02); never delete it from a release.
4. If the new launcher is a required upgrade, raise `launcher.minimumVersion` in the manifest in a follow-up PR.

> **Producing the first `.exe`.** The tag push (step 2) is the trigger. If you
> can't push a tag from your environment, either (a) run the **Launcher
> Release** workflow from the repo's **Actions** tab (the `workflow_dispatch`
> button — available once this workflow is on `main`), or (b) create the tag
> via GitHub's **Releases → Draft a new release → choose a new tag** UI. The
> resulting `.exe` (`AxoLauncher-Setup-X.Y.Z.exe`) is attached to a GitHub
> Release. For anyone other than the repo owner to download it, the repo must
> be **public**.

> **Both release workflows refuse a mismatched tag.** `launcher-vX.Y.Z` must
> equal `launcher/package.json` version, and `client-vX.Y.Z` must equal
> `mod_version` in `client/gradle.properties`. This is guarded because the
> launcher case fails silently otherwise: the build and publish both succeed,
> but `latest.yml` announces the old version, so no existing install ever sees
> the update. If a run fails on this step, bump the version file or retag —
> nothing was published.

### Promotion & rollback

- New Minecraft versions or risky changes land in the `beta` channel first — the channel is live in the manifest now, and the launcher shows channel tabs whenever more than one exists. Promotion to `stable` is a manifest edit moving/copying the Version entry.
- Version ids must be **unique across channels** (they name the install folder), so use a suffix: `1.21.11-r1` on stable, `1.21.11-b1` on beta. `validate.mjs` enforces this.
- Rollback = revert the manifest commit. Launchers pick up the old pins on next start; the client-side `.previous` jar (P3-05) covers users mid-session.

## Checklist before any tag

- [ ] CI green on `main`
- [ ] Version file bumped and matches the tag you're about to push
- [ ] For client tags: manifest update PR is prepared (step 4) so the release isn't orphaned
- [ ] `node manifest/validate.mjs --allow-placeholders` passes locally (full strictness after step 4)

The first execution of this document end-to-end is roadmap task **P4-07**; anything that required improvisation gets folded back into this file (P3-06).
