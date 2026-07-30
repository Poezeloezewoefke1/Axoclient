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
| Chat Timestamps | off | Dim [HH:mm] in front of each chat line |
| Chat Anti-Spam | off | Hides an exact repeat of the previous line within ~1s |
| Chat History | off | Keeps sent messages across servers and restarts |
| Freelook | off | Hold **Left Alt** to look around while you keep running the same way |

### Cosmetics

Ten bundled capes, twenty particle trails and auras, plus:

- **Custom Cape** — drop a 64×32 PNG into `<game dir>/axoclient/capes/` and
  enable it. The first PNG alphabetically is used, so naming one
  `1-favourite.png` picks it.
- **Colour Trail** — a trail in your accent colour, with size and density
  settings.
- **Halo** — a ring that turns above your head.
- **Wings** — two swept arcs behind your shoulders that follow your facing.

Halo and Wings are drawn with particles, not 3D models. A modelled accessory
needs a player-renderer mixin plus hand-built geometry — a lot of surface area
to break on a Minecraft update, and it looks wrong unless the model is
properly made. Particles give a clean readable shape for no port risk.

All cosmetics are local-only: they render on your screen, not other players'.
Showing them to anyone else would need a server hosting the textures, which
Axo deliberately does not have.

New modules are registered in one place — `AxoClient.onInitializeClient()` —
so the list grows without touching the GUI or config code.

## Module settings

Modules with something to tune show a small **>** on the right of their row
in the ClickGUI. Click it to fold the settings open, then use **-** and **+**.

Everything saves as you click. Nothing here needs the config file edited by
hand.

| Module | You can change |
| --- | --- |
| Crosshair | style (dot / cross / ring), size, centre gap |
| Zoom | zoomed FOV, how long the ease takes |
| Colour Trail | particle size, how often it emits |
| Halo | radius, number of points, how often it emits |
| Wings | span, points per wing, how often it emits |
| Any particle trail | how often it emits |

The Colour Trail's colour follows your ClickGUI accent, so picking a swatch
in the top bar recolours it. A fixed `0xRRGGBB` override is available in the
config file for anyone who wants one.

## Look and feel

The main menu uses Axo artwork instead of the vanilla rotating panorama, and
the Axo hub screen sits on the same image behind a dark panel.

Panels across the ClickGUI and HUD editor get a soft drop shadow, a light
top-edge gradient and bitten-out corners so they read as rounded. All of it is
drawn with `fill()` — no gradient or rounded-rect helper from vanilla — so the
styling cannot break on a Minecraft update.

### Changing the artwork

Replace one file:

```
client/src/main/resources/assets/axoclient/textures/gui/menu_background.png
```

Rules:

| | |
| --- | --- |
| Size | 1024×512 (declared in `MenuBackground.TEX_W/TEX_H` — update both if you change it) |
| Format | PNG |
| Aspect | Cropped to fill, never stretched, so any aspect works — the centre is what stays |

The image is darkened by a scrim so button text stays readable. If your art is
already dark, lower `SCRIM` in `MenuBackground`.

`GuiGraphics#blit` is the least stable signature in Minecraft's GUI code — it
has changed shape more than once inside 1.21 alone — so it is called from
exactly one place, `MenuBackground`. A port fixes one line, not every screen.

## Two mixin configs, on purpose

`axoclient.mixins.json` sets `required: true`. A mixin in there that stops
matching after a Minecraft update **stops the game from starting** — not just
that one feature. That is exactly the class of crash this project has already
hit once.

So there are two rules:

1. **Prefer no mixin at all.** Most features are built by polling and diffing
   state instead: the pickup log diffs your inventory, the combo counter
   watches attack timing, chat history uses the public `getRecentChat()` /
   `addRecentChat()` methods. None of those can break the game.
2. **When a mixin is genuinely needed for a nice-to-have, put it in
   `axoclient.optional.mixins.json`** (`required: false`,
   `defaultRequire: 0`). If it stops matching, the feature quietly stops
   working and the game still boots.

The chat, freelook and menu-background mixins live in the optional config for
exactly this reason. The five mixins in the required config are the ones the
client genuinely cannot work without.

The menu background is a good example of the split paying off. It has two
injections, each allowed to fail alone:

- panorama suppression fails → the vanilla panorama returns and hides the art
- the draw call fails → you get the plain vanilla menu

Neither outcome stops the game starting, so neither belongs in the required
config.

### Freelook's extra safety

Freelook needs two mixins that must work *together*: one banks the mouse
movement, the other points the camera. If only the first applied, the mouse
would be swallowed with nothing moving in its place — a frozen view, which is
worse than the feature not existing.

So the camera mixin sets a flag the first time it runs, and the mouse hook
refuses to swallow anything until that flag is set. A half-applied pair
therefore degrades to "freelook does nothing", and the module says so with a
notification rather than leaving a dead key.

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
