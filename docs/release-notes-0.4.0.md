# Axo Client 0.4.0 — release notes (draft, not shipped)

Everything below is on `claude/session-context-reminder-jrohrm`, builds
against Minecraft 1.21.11 in CI, and has **not been tried in the game yet**.
Run the checklist at the bottom before tagging. The release steps themselves
are in `releasing.md`.

## For players

**Big new features**

- **Waypoints.** Press **B** to mark a spot; the HUD points you back with an
  arrow, distance and height difference. **N** cycles, **J** opens a list to
  rename, point to or delete. A **Death** waypoint is dropped automatically
  when you die. Saved per server/world and per dimension.
- **Minimap.** A map of the area around you in a screen corner, with your
  heading and your waypoints. Zoom and size are adjustable.
- **Hover descriptions** on every module in the menu, and search now matches
  them ("spawn" finds Light Level).
- **HUD Style.** Recolour all HUD text, add a dark backing box, turn shadows
  off.
- **Performance tab** is no longer empty: **Performance Mode** (lighter
  graphics in one switch, your settings restored when off), **Stutter
  Alert**, **Ping Graph**, plus FPS Counter, FPS Graph and Memory.
- **Crash protection.** If a module hits an error it is switched off with a
  notification instead of crashing the game.

**New modules (42)**

- *HUD:* Light Level, Nether Coords, Session Stats, Chunk Info, Weather, Day
  Cycle, Block Info, Hostile Counter, Depth Guide, Fall Warning,
  Inventory Full, Saturation, Elytra Info, Mount Info, Movement State, Hunger
  Warning, Armor Points, Armor Icons, HUD Style, Minimap
- *Combat:* Reach Display, Item Counter, Low Health Warning, Bow Charge,
  Nearby Players, Hit Sound, Pearl Cooldown, Totem Warning, Kill Counter
- *Quality of life:* Waypoints, Stopwatch, Auto Walk, Break Reminder, Effect
  Expiry Warning, Sleep Reminder, Chat Mentions, Food Tooltips, Anvil Cost
  Tooltips
- *Performance:* Performance Mode, Stutter Alert, Ping Graph, FPS Graph

**Improvements to existing modules**

- Keystrokes shows clicks per second on the mouse keys; optional space bar.
- Crosshair turns red (or white/gold) on a mob or player; style is a proper
  Dot / Cross / Ring picker.
- Durability Warning plays a sound and notifies once per item.
- Clock can be 12-hour; Coordinates has a decimals and a facing option;
  Speed can show km/h.
- Binding a key that's already in use now tells you what else uses it.
- The second "Clock" module (an exact duplicate) is now **Game Clock**,
  showing in-game time.

## Before shipping: in-game checklist

CI proves this compiles against 1.21.11 and passes the unit tests. It can't
prove anything looks right. In a dev client (`./gradlew runClient`) or a
test install, check:

- [ ] Game boots with the menu showing about 90 modules; hovering a card shows its description.
- [ ] **Waypoints:** B marks, the arrow points the right way while turning
      (it's maths-tested, but check on screen), N cycles, J opens the list,
      renaming with Enter works, dying creates a Death waypoint.
- [ ] Arrow glyphs (↑ ↗ → …) render in Minecraft's font, not as boxes.
- [ ] **Minimap:** terrain colours look like a vanilla map, north is up, the
      player tick points where you face, waypoints appear as dots, all three
      zoom levels work, nothing drawn in the Nether.
- [ ] **Performance Mode:** on → clouds/particles/shadows drop; off → your
      exact previous video settings return.
- [ ] **Low Health Warning** red edge isn't overpowering at 1 heart.
- [ ] **HUD Style** background box lines up with the text.
- [ ] **Chat Mentions:** someone saying your name pings; your own messages don't.
- [ ] **Hit Sound** plays on a landed hit and not on a miss.
- [ ] Turn every module on at once for a minute: no error notifications
      ("… hit an error and was turned off"). If one appears, `latest.log`
      has the stack trace under `Module '<id>' failed`.

Then follow `releasing.md`: bump `mod_version` to `0.4.0`, tag
`client-v0.4.0`, point the manifest at the release, and update the website's
features and changelog pages in the same change (pushing `website/`
publishes it, so do that only once the release is live).
