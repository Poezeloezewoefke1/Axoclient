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

Press **Right Shift** in-game to open it. Modules are laid out as a grid of
cards, and the theme, accent colour and favourites persist between sessions.

- **Category tabs** along the top (All, PvP, Performance, Quality of Life,
  Cosmetic, HUD) narrow the grid to one category.
- **Hover a card** to see a one-line description of what the module does.
- **Click Enabled / Disabled** on a card to toggle it. Enabled cards get an
  accent outline.
- **Gear button** opens that module's settings (see below).
- **Right-click a card** to bind a toggle key — the next key you press
  becomes its hotkey (press **Esc** while binding to clear it). The bound key
  shows under the module name.
  If the key is already used — by a Minecraft control or another Axo key —
  a notification names what else it's bound to. The bind still goes
  through; whether a clash matters is your call.
- **Star** a card to favourite it. Favourites sort first, and the star
  button next to the search box shows only favourites.
- **Search box** filters live as you type, matching names and descriptions,
  so searching "spawn" finds Light Level.
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
| Coordinates | on | X / Y / Z and facing direction (decimals and facing in settings) |
| CPS Counter | on | Left/right clicks per second (1s window) |
| Speed | off | Blocks per second or km/h |
| Ping | off | Latency to the server |
| Health / Hunger / Experience | off | Exact numbers instead of counting icons |
| XP Progress | off | Percentage to the next level |
| Direction | off | Facing as N/E/S/W plus yaw |
| Compass | off | Compass strip across the top of the screen |
| Biome | off | Biome you're standing in |
| Clock | on | Real-world time, 24- or 12-hour |
| Game Clock | off | In-game time of day as a 24-hour clock |
| Day Counter | off | In-game day number |
| Session Uptime | off | How long this session has run |
| Armour | off | Each armour piece with durability left |
| Armor Icons | off | Armour and held items as item icons with durability (or stack size), green to red |
| Potion Effects | off | Active effects and time remaining |
| Server Info | off | Server name and address |
| TPS | off | How well the server is keeping up |
| Death Coords | off | Where you last died |
| Durability Warning | off | Warns before a tool breaks, with a one-time sound and notification (can be turned off) |
| Pickup Log | off | Short list of what you just picked up |
| Ping Spike | off | Flags the moment latency jumps |
| Crosshair | off | Dot / cross / ring in your accent colour; turns red (or white/gold) on a mob or player |
| Light Level | off | Block and sky light where you stand; says when mobs can spawn |
| Nether Coords | off | Your matching Nether position (or Overworld, in the Nether) |
| Session Stats | off | Distance travelled and deaths this session |
| Chunk Info | off | Chunk, position inside it, and region file |
| Weather | off | Clear / raining / thunderstorm, even underground, with a notification when it changes |
| Day Cycle | off | Real-time countdown to nightfall or sunrise |
| Block Info | off | Name and coordinates of the block you're looking at |
| Hostile Counter | off | Hostile mobs within a set radius |
| Depth Guide | off | Your Y level and which ores are richest there |
| Fall Warning | off | How far you're falling and the worst-case damage |
| Inventory Full | off | Warns when there's no free slot left |
| Saturation | off | The hidden food stat that decides when you get hungry |
| Elytra Info | off | While gliding: speed, pitch and elytra durability |
| Mount Info | off | What you're riding, its real speed and health |
| Movement State | off | Sprinting / sneaking / swimming / flying / gliding |
| Hunger Warning | off | Tells you to eat; separate warning when you're too hungry to sprint |
| Armor Points | off | Total armour and armour toughness |
| HUD Style | off | Restyle all HUD text: colour (six choices), dark background box, shadow on/off |
| Minimap | off | North-up map of the area around you with your heading and waypoints; corner, zoom and size in settings |
| Module List | on | Which modules are active |

### Combat

| Module | Default | What it does |
| --- | --- | --- |
| Keystrokes | on | WASD + mouse key overlay; mouse keys show clicks per second while clicking, optional space bar |
| Target HUD | off | Who you're looking at, with a health bar |
| Attack Cooldown | off | Weapon charge bar |
| Damage Numbers | off | Floating damage dealt |
| Combo Counter | off | Consecutive hits |
| Reach Display | off | How far your last hit landed from |
| Item Counter | off | Totems, pearls, golden apples and arrows left |
| Low Health Warning | off | Pulsing red screen edge at low health (threshold in settings) |
| Bow Charge | off | How drawn your bow is; FULL means a critical shot |
| Nearby Players | off | Closest players with distance, health and ping |
| Hit Sound | off | A sound only you hear when a hit lands (three to choose from) |
| Pearl Cooldown | off | Ender pearl cooldown remaining |
| Totem Warning | off | Warns when you carry totems but none is in your hands, and announces each totem used with how many are left |
| Combat Timer | off | Counts down from your last hit given or taken, to match a server's combat tag (length in settings) |
| Kill Counter | off | Kills this session, players counted separately (client-side estimate) |

### Performance

| Module | Default | What it does |
| --- | --- | --- |
| FPS Counter | on | Frames-per-second readout |
| FPS Graph | off | Frame-time graph in the bottom-right; stutters show as spikes |
| Memory | off | Heap used / total |
| Performance Mode | off | Clouds off, minimal particles, no entity shadows or biome blend, shorter entity distance — your settings come back when you turn it off |
| Stutter Alert | off | Notification when one frame takes longer than a set time (250 ms by default) |
| Ping Graph | off | Your ping over the last minute, sampled each second; sits above the FPS graph (hidden in singleplayer) |
| Entity Count | off | Entities your client has loaded, as on F3 |

Performance Mode writes your own video settings to `config/axoclient.json`
before changing anything, so even if the game crashes while it's on, turning
it off later still restores them.

### Quality of life

| Module | Default | What it does |
| --- | --- | --- |
| Fullbright | off | Maximum brightness without touching your video settings |
| Zoom | on | Hold **C** to zoom, eased per frame; scroll while holding to zoom further in or out (rebindable) |
| Toggle Sprint | off | Sprint stays on without holding the key |
| Copy Coords | on | **F8** copies your position to the clipboard (rebindable) |
| Chat Timestamps | off | Dim [HH:mm] in front of each chat line |
| Chat Anti-Spam | off | Hides an exact repeat of the previous line within ~1s |
| Chat Mentions | off | Gold » marker and a ping when someone says your name (not your own messages) |
| Chat History | off | Keeps sent messages across servers and restarts |
| Freelook | off | Hold **Left Alt** to look around while you keep running the same way (rebindable, hold or toggle) |
| Clear Water & Lava | off | Removes the full-screen liquid overlays |
| Durability Tooltips | on | Exact durability on damageable items, coloured by wear |
| Shulker Tooltips | on | Lists a shulker box's contents on its tooltip |
| Food Tooltips | on | Hunger and saturation restored, on every food |
| Anvil Cost Tooltips | on | An item's anvil work penalty, red when it's close to Too Expensive |
| Waypoints | off | **B** marks a spot, **N** cycles, **J** opens the list to rename or delete; the HUD shows an arrow and distance back (all rebindable) |
| Stopwatch | off | **K** starts and pauses; reset key in settings |
| Auto Walk | off | Keeps walking forward; bind a key to tap it on and off |
| Break Reminder | off | A notification every hour of play (interval in settings) |
| Effect Expiry Warning | off | Notification 10 seconds before a potion effect runs out |
| Sleep Reminder | off | Notification the moment beds start working |

### Cosmetics

Four cosmetic modules, each with its variant chosen in its own settings:

- **Capes** — ten bundled colours, picked with the **Cape** setting.
- **Trails** — twenty-two trails and auras, picked with the **Trail** setting.
- **Halo** — a ring that turns above your head.
- **Wings** — two swept arcs behind your shoulders that follow your facing.
- **Custom Cape** — drop a 64x32 PNG into `<game dir>/axoclient/capes/` and
  enable it. The first PNG alphabetically is used, so naming one
  `1-favourite.png` picks it.

Capes and Trails used to be thirty-two separate toggles. They were really two
decisions, and nothing stopped you enabling three capes at once — "last one
enabled wins" was the only thing resolving it. One toggle with a picker is
both smaller in the menu and unambiguous.

Halo and Wings are still drawn with particles rather than 3D models. Making
them modelled geometry is planned and not done yet — see
`docs/3d-cosmetics-plan.md` for what has been established and what has to be
resolved first.

All cosmetics are local-only: they render on your screen, not other players'.
Showing them to anyone else would need a server hosting the textures, which
Axo deliberately does not have.

### Minimap

A north-up map in a screen corner (bottom right by default), coloured like a
vanilla map from the top block of each column your client has loaded.
Higher ground is drawn lighter and lower ground darker, so hills and
riverbanks read at a glance. The white dot and tick are you and your
heading; gold dots are your waypoints, with the one the HUD arrow follows
in your accent colour. Waypoints off the edge of the map stay pinned to the
edge in their direction.

Settings: **Corner**, **Zoom** (64, 128 or 256 blocks across) and **Size**.
If you also use the FPS or Ping graph, which sit bottom right, move the
minimap to another corner.
The map refreshes about twice a second, spread over several ticks so it
never costs you a frame. It's hidden in the Nether, where the top block
of every column is the bedrock roof.

### Waypoints

Press **B** to save where you're standing. The HUD then points to it:

```
↗ Waypoint 1  152m  +12y
```

The arrow is which way to turn, then the distance across the ground and how
far up or down it is (shown once the difference is 3 blocks or more). **N**
cycles through your waypoints. **Remove key** deletes the one being shown; it
has no key until you set one, so you can't delete a waypoint by accident.

Press **J** to open the waypoint list for the world you're in. Click a name
to rename it (Enter to save), **Point** to make the HUD arrow follow it, or
**Delete** to remove it. **+ Add here** saves your position and lets you name
it straight away. Waypoints in your current dimension are listed first; ones
in other dimensions are shown dimmed, since the arrow can't point across
dimensions.

When you die, a **Death** waypoint is placed where it happened, replacing the
previous one for that world. Turn this off with **Mark deaths** in the
module's settings.

Waypoints are saved separately for each server or singleplayer world and
each dimension. You only cycle through the ones for where you are, so a
Nether waypoint never points you across the Overworld. They live in
`config/axoclient.json` under `waypoints.points`.

They appear on the HUD only, with no beam or marker in the world. That keeps
them free of world rendering, the same constraint that keeps Halo and Wings
as particles for now.

New modules are registered in one place — `AxoClient.onInitializeClient()` —
so the list grows without touching the GUI or config code.

## Module settings

Each module card in the mod menu has a gear button. Click it for that
module's settings, then use **-** and **+** to change a value. Pickers (Cape,
Trail, and so on) cycle when you click the middle of the value. Key settings
are rebound by clicking them and pressing the key you want.

Everything saves as you click. Nothing here needs the config file edited by
hand.

| Module | You can change |
| --- | --- |
| Crosshair | style (dot / cross / ring), size, centre gap |
| Zoom | zoomed FOV, ease time in milliseconds, key |
| Capes | which cape |
| Trails | which trail, how often it emits |
| Halo | radius, number of points, how often it emits |
| Wings | span, points per wing, how often it emits |
| Copy Coords | key, coordinate format, where it confirms |
| Freelook | key, hold or toggle |
| Clear Water & Lava | water, lava and powder snow overlays separately |
| Durability Tooltips | absolute, percentage, or both |
| Shulker Tooltips | how many lines, whether to merge duplicate stacks |

The Themed Dust trail follows your ClickGUI accent, so picking a swatch in
settings recolours it. A fixed `0xRRGGBB` override is available in the config
file for anyone who wants one.

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

The chat, freelook, menu-background and zoom-scroll mixins live in the
optional config for exactly this reason. The five mixins in the required config are the ones the
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
