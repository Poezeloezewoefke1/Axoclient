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

| Module | Category | Default | What it does |
| --- | --- | --- | --- |
| FPS Counter | HUD | on | Frames-per-second readout |
| Coordinates | HUD | on | X / Y / Z and facing direction |
| CPS Counter | HUD | on | Left/right clicks per second (1s window) |
| Keystrokes | PvP | on | WASD + mouse key overlay |
| Fullbright | Quality of Life | off | Maximum brightness without touching your video settings |
| Zoom | Quality of Life | on | Hold **C** to zoom the view |

New modules are registered in one place — `AxoClient.onInitializeClient()` —
so the list grows without touching the GUI or config code.

## Notifications

Toggling a module raises a small animated toast in the top-right corner
(green for enabled, neutral for disabled). Any module can raise one via
`Notifications.push(...)`.

## Where settings live

All of the above persists to `config/axoclient.json` in your instance folder
(atomic writes, so a crash never corrupts it). The launcher gives each
installed version its own instance directory, so per-version layouts don't
collide.
