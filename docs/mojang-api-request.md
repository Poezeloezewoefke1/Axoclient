# P0-03 — Mojang Minecraft API Access Request (submission pack)

Everything needed to submit the third-party API approval request for our Azure app.
Fill the form from the tables below — it should take under five minutes.

## Where to submit

Open https://minecraft.wiki/w/Microsoft_authentication in your browser and follow the
link to the **Minecraft API app review form** (historically `https://aka.ms/mce-reviewappid`).
Use the link from the wiki page — it is kept current; the aka.ms shortlink occasionally moves.
Sign into the form with the same Microsoft account that owns the Azure app.

## Copy-paste answers

| Form field (typical) | Answer |
|---|---|
| Application / product name | Axo Launcher |
| Azure Application (client) ID | `77802178-1e6b-4163-b382-d4095833986e` |
| Directory (tenant) ID, if asked | `fddcd525-ab75-462a-941b-cbbc4baec66c` |
| Contact email | the email on your Microsoft account (must be one you actually read — approval arrives by email) |
| Website | the GitHub repository URL for now; replace with the site once P4-04 deploys |
| OAuth flows used | Authorization code with PKCE (public client, desktop) and device code flow |
| Redirect URI(s) | `http://localhost` |

**Description (paste as-is, adjust freely):**

> Axo Launcher is a free desktop launcher for Minecraft: Java Edition on Windows. It signs
> players in through the official Microsoft OAuth flow, verifies game ownership via the
> Minecraft services API, and launches the game with the player's own account. It supports
> only legitimate, paid accounts — there is no offline or non-Microsoft login. Refresh
> tokens are stored encrypted on the user's own device and never transmitted to us; we
> operate no account servers. The launcher installs a Fabric-based client modpack focused
> on HUD, performance, and quality-of-life features, and is not affiliated with Mojang or
> Microsoft.

## After submitting

1. Record the submission date in `docs/decisions.md` (O-7).
2. Expect a wait of several weeks; the result arrives by email. Silence for 6+ weeks is
   common — a polite resubmission is the usual remedy.
3. On approval: flip `USE_AXO_CLIENT_ID` to `true` in `launcher/src/main/auth.ts`,
   test a real login (P2-08 matrix), and record the approval date in O-7.
4. If rejected: the rejection email states the reason; typical fixes are clarifying the
   description (no account sharing, no server-side token storage) and resubmitting.
   Meanwhile nothing breaks — logins continue on msmc's shared client ID.

## Azure app checklist (must all be true before submitting)

- [x] Supported account types: **Any Entra ID tenant + personal Microsoft accounts**
- [x] Allow public client flows: **Yes**
- [ ] Redirect URI `http://localhost` present under **Mobile and desktop applications** — verify saved!
- [ ] All Authentication-page changes **saved** (Azure discards unsaved edits on navigation)
