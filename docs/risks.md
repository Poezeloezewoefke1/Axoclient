# Axo Client — Risks & Hard Parts

Ordered by (impact × likelihood). Each has a mitigation that is already reflected in the architecture or roadmap.

## R1 — Microsoft/Mojang API approval lead time · **start now**
Third-party launchers need their Azure app ID approved by Mojang for the Minecraft services API; the wait is weeks and opaque. Until approval, logins ride on library-default client IDs — fine for development, not acceptable for shipping.
**Mitigation:** P0-02 + P0-03 are the first roadmap tasks; all auth code takes the client ID from config so swapping to the approved ID is a one-line change. **This is the only external gate on the MVP — submit before writing more code.**
Refs: https://minecraft.wiki/w/Microsoft_authentication · https://github.com/RaphiMC/MinecraftAuth (background on flows/IDs)

## R2 — Mod redistribution licensing
Sodium (PolyForm Shield) and others restrict redistribution; clients have had public disputes over bundling.
**Mitigation (architectural, non-negotiable):** the launcher downloads mods from Modrinth's CDN on the user's machine at install time; our releases contain only our own code. The manifest encodes this (`source: "modrinth"` only). Never "vendor" a mod jar into the repo or a release.

## R3 — Windows SmartScreen on unsigned installers
Unsigned NSIS installers show scary warnings; conversion on the download page will suffer.
**Mitigation:** P5-07 decision task (OV cert or Azure Trusted Signing ~$10/mo); until then the website documents the "More info → Run anyway" path with honest wording. Budget item, not engineering item.

## R4 — Minecraft update churn
Every MC release can break mappings, mixins, and bundled-mod availability; clients live or die by port speed.
**Mitigation:** Phase 6 exists precisely for this — adapter layer (P6-02), porting checklist (P6-06), data-driven manifest so old versions keep working while a port is in progress. Bundled-mod choices are per-version in the manifest, so a missing Sodium port doesn't block a release (ship without it in that version's entry).

## R5 — Dependency health: minecraft-launcher-core & msmc
Both are community projects; maintenance can lapse, and MC launch internals change (e.g. new asset/library formats, auth tweaks).
**Mitigation:** both are isolated behind our own modules (`launch.ts`, `auth.ts`) with narrow interfaces — swappable for a custom implementation (the Java references OpenLauncherLib/MinecraftAuth document the protocol if we must go native). Pin exact versions; upgrade deliberately.

## R6 — Server anti-cheat / fair-play perception
"PvP client" reads as "hack client" to server admins if features cross the line.
**Mitigation:** hard feature line documented in architecture.md (info + rendering only; no reach/aim/autoclick — the Lunar/Badlion line). Review every PvP module against the rule at PR time. The website copy should say this out loud.

## R7 — EULA/branding compliance
Mojang's EULA and brand guidelines constrain naming and monetization; "not affiliated" disclaimers are expected.
**Mitigation:** P4-02 bakes the disclaimer into the footer; no monetization in scope; never use Mojang/Minecraft logos in branding (P0-01).

## R8 — Solo-dev scope creep
Three codebases is a lot for one person; clients die from half-finished feature sprawl.
**Mitigation:** the MVP non-goals table in mvp.md is the contract; every "wouldn't it be cool" becomes a filed task, and phases close before the next opens (M-gates).

## Hard parts (not risks, just genuinely tricky)

1. **First full launch pipeline (P2-09→P2-13):** four network-dependent stages that must compose idempotently. Budget calm time; the progress/error surface (P2-14) is what makes it debuggable.
2. **Auth edge cases (P2-08):** accounts without Minecraft, child accounts, expired tokens — the test matrix in the task exists because each has burned other launchers.
3. **Atomic-ish updates (P3-04/05):** replacing jars while guaranteeing a bootable state; the `.previous` rollback file is the safety net.
4. **Keeping the manifest honest (P3-08):** placeholder hashes must never reach production — CI validation is the guard.

## Decisions you (the owner) still need to make

Tracked in [`decisions.md`](decisions.md) as OPEN: license for our own code (P0-05), final branding/logo (P0-01), domain name (P4-04), code signing budget (P5-07), analytics stance (P5-08), multi-version build strategy (P6-03).
