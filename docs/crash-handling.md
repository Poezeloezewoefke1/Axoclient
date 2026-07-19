# Axo Client — Crash Handling Plan (P5-01)

Taxonomy of failure modes, how each is detected, what the user sees, and where the evidence lands. Implementation tasks: P5-02 (game logs), P5-03 (launcher crash reporter), P3-05 (boot-crash rollback).

## Failure taxonomy

| # | Failure | Detection signal | User-facing response | Evidence |
|---|---|---|---|---|
| 1 | Launcher main-process crash | uncaught exception / unhandled rejection in main | dialog: "Axo Launcher hit a problem" + button opening GitHub issues with prefilled title; app exits cleanly | `logs/launcher.log` + crash report file `logs/crash-<ts>.txt` |
| 2 | Launcher renderer crash | `render-process-gone` event | window reload once; if it recurs within 60 s, fall to case 1 dialog | `logs/launcher.log` |
| 3 | Install/launch pipeline failure (network, disk, hash) | thrown error in `game:launch` IPC | error card on Home with plain-language message + Retry + "Open logs" | `logs/launcher.log` (every stage is logged with timestamps) |
| 4 | Game crashes at boot (< 60 s after spawn) | child exit code ≠ 0 with runtime < 60 s | "Game crashed while starting" card; offer **Repair install** and, if a `.previous` client jar exists, **Roll back** (P3-05) | `logs/game-<ts>.log` + Minecraft's own `crash-reports/` in the install dir |
| 5 | Game crashes mid-session | child exit code ≠ 0 after 60 s | "Game crashed" card with "Open log folder"; no auto-action (mods state is fine) | same as 4 |
| 6 | Game hangs (no exit, no logs) | user-initiated only — "Force close" button shown while stage=running | confirm dialog → SIGKILL the child; treated as case 5 afterwards | tail of `logs/game-<ts>.log` |
| 7 | Corrupted install (hash mismatch on launch sync) | sha1 mismatch during sync — auto-repaired by design | silent re-download; only surfaces (case 3) if the re-download also fails | `logs/launcher.log` |

## Rules

1. **Every failure has a log line before it has UI.** No error card without a corresponding entry in `launcher.log`.
2. **No auto-upload.** Crash data stays on the user's machine; the reporter only *offers* to open a prefilled GitHub issue (privacy-first, decision O-5 territory).
3. **Exit code 0 is never treated as a crash**, even after a short run — the user may simply have quit.
4. **Log budget:** keep the last 5 `game-*.log` files and rotate `launcher.log` at 5 MB (implementation detail of P5-02).
5. **The 60-second boot threshold** is a constant in one place, not sprinkled through the code.

## Current status

- Case 3 and 7 behavior already exists (pipeline error → Home error card + Retry; sync auto-repair).
- Cases 1, 2, 4, 5, 6 are P5-02/P5-03 implementation work; case 4's rollback needs P3-05.
