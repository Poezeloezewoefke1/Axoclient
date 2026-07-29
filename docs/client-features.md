# Axo Client — In-game features & controls

Everything below ships in the Fabric mod today. Nothing here needs the
launcher — drop the built jar in your `mods/` folder (with Fabric API) and
these work in a normal Minecraft 1.21.11 instance.

## Opening the UI

| Key | Action |
| --- | --- |
| **Right Shift** | Open / close the **ClickGUI** (the main control panel) |
| **C** (hold) | **Zoom** (when the Zoom module is on — it is by default) |
| Any key you bind | Toggle the module you bound it to (see *Keybinds* below) |

## ClickGUI

Press **Right Shift** in-game to open it. The panel is fully themeable and
its layout persists between sessions.

- **Category panels** — one per category (PvP, Performance, Quality of Life,
  HUD). Drag a panel by its title bar to move it; positions are saved.
- **Left-click a module** to toggle it on/off. Enabled modules show an accent
  bar and coloured name.
- **Right-click a module** to bind a toggle key — the next key you press
  becomes its hotkey (press **Esc** while binding to clear it). The bound key
  shows as a badge on the module row.
- **Search box** (top-left) filters modules live as you type.
- **Dark / Light** button toggles the theme.
- **HUD** button opens the HUD editor (below).
- **Accent swatches** pick the highlight colour. The choice — like the theme
  and every panel position — is written to `config/axoclient.json`.

## HUD editor

Open the ClickGUI and click **HUD**. Every HUD element becomes a draggable
handle:

- **Drag** a handle anywhere. On release it **snaps to the nearest of nine
  anchors** (corners, edges, centre) and stores an offset, so your layout
  survives resolution and GUI-scale changes.
- **Arrow keys** nudge the selected handle by 1px (hold **Shift** for 10px).
- **R** resets the selected element to its default position.
- Disabled HUD modules appear **dimmed** so you can place them before turning
  them on.
- **Esc** closes the editor.

## Modules

Everything is a module. Toggle any of them in the ClickGUI.

### HUD

| Module | Default | What it does |
| --- | --- | --- |
| FPS Counter | on | Frames-per-second readout |
| Coordinates | on | X / Y / Z and facing direction |
| CPS Counter | on | Left/right clicks per second (1s window) |
| Memory | off | Heap used / total |
| Speed | off | Blocks per second |
| Ping | off | Latency to the server |
| Health / Hunger / Experience | off | Exact numbers instead of counting icons |
| XP Progress | off | Percentage to the next level |
| Direction | off | Facing as N/E/S/W plus yaw |
| Compass | off | Compass strip across the top of the screen |
| Biome | off | Biome you're standing in |
| Clock | off | Real-world time |
| Day Counter | off | In-game day number |
| Session Uptime | off | How long this session has run |
| Armour | off | Each armour piece with durability left |
| Potion Effects | off | Active effects and time remaining |
| Server Info | off | Server name and address |
| TPS | off | How well the server is keeping up |
| Death Coords | off | Where you last died |
| Durability Warning | off | Warns before a tool breaks |
| Pickup Log | off | Short list of what you just picked up |
| Ping Spike | off | Flags the moment latency jumps |
| Crosshair | off | Dot / cross / circle in your accent colour |
| Module List | on | Which modules are active |

### Combat

| Module | Default | What it does |
| --- | --- | --- |
| Keystrokes | on | WASD + mouse key overlay |
| Target HUD | off | Who you're looking at, with a health bar |
| Attack Cooldown | off | Weapon charge bar |
| Damage Numbers | off | Floating damage dealt |
| Combo Counter | off | Consecutive hits |

### Quality of life

| Module | Default | What it does |
| --- | --- | --- |
| Fullbright | off | Maximum brightness without touching your video settings |
| Zoom | on | Hold **C** to zoom, with smooth easing |
| Toggle Sprint | off | Sprint stays on without holding the key |
| Unfocused FPS | off | Caps frames when the window isn't focused |
| Copy Coords | on | **F8** copies your position to the clipboard |

### Cosmetics

Ten bundled capes, twenty particle trails and auras, plus **Custom Cape** —
drop a 64×32 PNG into `<game dir>/axoclient/capes/` and enable it. The first
PNG alphabetically is used, so naming one `1-favourite.png` picks it.

All cosmetics are local-only: they render on your screen, not other players'.
Showing them to anyone else would need a server hosting the textures, which
Axo deliberately does not have.

New modules are registered in one place — `AxoClient.onInitializeClient()` —
so the list grows without touching the GUI or config code.

### Why almost nothing here uses a mixin

`axoclient.mixins.json` sets `required: true`. A mixin that fails to apply
after a Minecraft update **stops the game from starting** — not just that one
feature. So features are built by polling and diffing state where possible
(the pickup log diffs your inventory; the combo counter watches attack
timing) and the mixin list is kept to five. Chat-based features (timestamps,
anti-spam, cross-server history) are the main things this rules out for now:
they have no non-mixin route.

## Notifications

Toggling a module raises a small animated toast in the top-right corner
(green for enabled, neutral for disabled). Any module can raise one via
`Notifications.push(...)`.

## In-game updates

On startup the client checks the version manifest (the same one the launcher
uses) and, if a newer build exists, **downloads it into
`<gameDir>/axoclient-updates/`** and raises a notification. A running mod jar
can't replace itself, so the update applies on your next launch — the
launcher re-syncs the mods folder automatically, or you can drop the staged
jar into `mods/` yourself. Disable via the `update.check` / `update.download`
config keys.

## Where settings live

All of the above persists to `config/axoclient.json` in your instance folder
(atomic writes, so a crash never corrupts it). The launcher gives each
installed version its own instance directory, so per-version layouts don't
collide.
