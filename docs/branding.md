# Axo Client — Branding Direction (P0-01)

Style: **clean / professional**. Light blue on black. No neon clutter, no gamer-edgelord aesthetics — the brand should read as "polished tool", closer to a developer product than a hack client.

## Name usage

- Full product name: **Axo Client** (always two words, both capitalized).
- Short form in UI where space is tight: **Axo** (e.g. "Updating Axo…").
- The launcher is **Axo Launcher**; never call the launcher "Axo Client".
- Never style as "AXO Client" in prose — all-caps `AXO` is reserved for the logotype.
- Required legal line wherever the product is distributed (website footer, launcher login screen, store listings):
  > Axo Client is not an official Minecraft product and is not affiliated with, or approved by, Mojang or Microsoft.

## Color tokens (single source of truth)

These exact values live in `launcher/src/renderer/src/styles.css` and `website/styles.css`; any change happens in all places in one commit.

| Token | Value | Use |
|---|---|---|
| `--axo-blue` | `#38bdf8` | primary accent: CTAs, logo, active states, HUD default |
| `--axo-blue-dim` | `#0ea5e9` | hover/pressed state of the accent |
| `--axo-black` | `#0a0a0f` | app + page background |
| `--axo-surface` | `#12121a` | cards, sidebar |
| `--axo-surface-2` | `#1a1a24` | inputs, nested surfaces |
| `--axo-border` | `#26263a` | 1px hairlines |
| `--axo-text` | `#e7e9ee` | primary text |
| `--axo-text-muted` | `#8b90a0` | secondary text |
| `--axo-error` | `#f87171` | errors only — never decorative |

Rule: the accent blue is for *interaction and identity*, not decoration. If a screen is >10% blue, it's too much.

In-game HUD uses ARGB `0xFF38BDF8` (see `FpsHudModule.AXO_BLUE`) as the default text color.

## Typography

- UI + website: system stack `'Segoe UI', system-ui, sans-serif` — native on the Windows target, zero font loading.
- Logotype: bold 800 weight, letter-spaced (`AXO` + small `CLIENT` sub-mark), rendered in CSS today. When a real logo asset exists it replaces the CSS logotype everywhere at once.
- Optional later upgrade: self-hosted Inter for the website only. Never load fonts from third-party CDNs in the launcher.

## Logo brief (open decision O-2)

- Motif: an **axolotl** — geometric/minimal (think: abstracted head with gills as three prongs), single accent color on black, must survive at 16×16 (favicon) and 256×256 (installer icon).
- Deliverables when commissioned/created: `assets/icon.png` (1024²), `assets/icon.ico` (multi-size), monochrome variant for dark/light contexts.
- Constraint: no visual resemblance to Mojang/Minecraft trademarks (no creeper faces, no grass blocks, no Minecraft font).

## Tone of voice

- Direct and calm. "Update ready — Restart" not "🔥 New update dropped!!".
- Errors are honest and actionable: say what failed and what the user can do; never blame the user.
- The fair-play stance is stated proudly on the website (see risks R6): allowed-on-servers is a selling point, phrased positively.
