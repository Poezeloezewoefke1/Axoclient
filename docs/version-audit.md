# Axo Client — Version-Coupling Audit (P6-01)

Every touchpoint between Axo code and Minecraft internals, classified by how likely it is to break on a Minecraft version bump. Generated 2026-07-19 against 1.21.11 (Mojang mappings). Re-run the audit (grep `net.minecraft|com.mojang|org.lwjgl` imports) whenever Phase 6 work starts.

Classification:
- **STABLE** — API surface that has survived many versions; port risk low.
- **WATCH** — has changed within the 1.21.x line already, or sits near active refactors; check on every port.
- **MIXIN** — direct injection into vanilla internals; must be re-validated against new mappings on every port. Already isolated in `mixin/`.

| Touchpoint | Used by | Class | Notes |
|---|---|---|---|
| `Minecraft.getInstance()`, `.options`, `.player`, `.screen`, `.setScreen`, `.font`, `.getFps()` | most modules, core | STABLE | Core accessors, very long-lived. |
| `GuiGraphics.drawString/fill/drawCenteredString/guiWidth/guiHeight` | HUD system, settings screen | WATCH | The GUI render stack was reworked in 1.21.6+; signatures held so far, but this is the most active area of MC rendering. |
| `Gui.render(GuiGraphics, DeltaTracker)` injection point | `mixin/GuiMixin` | MIXIN | Verified compiling on 1.21.11; re-check the method signature on every bump. |
| `OptionInstance` private field `value` | `mixin/OptionInstanceAccessor` (Fullbright) | MIXIN | Field-name dependent; if renamed, Fullbright silently breaks — port checklist must include it. |
| `Options.gamma()/fov()/keyAttack/keyUse/keyUp/keyDown/keyLeft/keyRight` | Fullbright, Zoom, CPS, Keystrokes | STABLE | Option/keybinding fields stable across 1.19→1.21. |
| `LocalPlayer.getX/getY/getZ/getDirection` | Coordinates | STABLE | Entity position API, very long-lived. |
| `InputConstants.isKeyDown(Window, int)` | `util/Keys` | WATCH | **Already changed within 1.21.x** (used to take a long handle). Single call site by design. |
| `Screen`, `Button.builder(...)`, `Component.literal` | `ui/AxoSettingsScreen` | WATCH | Widget API changes every few versions (last big break 1.19.4); screen is deliberately vanilla-minimal. |
| `Font.width/lineHeight` | HUD | STABLE | Unchanged for years. |
| `org.lwjgl.glfw.GLFW` key constants | Keys, Zoom, AxoClient | STABLE | LWJGL constants, MC-version independent. |
| Fabric API: `ClientTickEvents.END_CLIENT_TICK` | `AxoClient` | STABLE | Fabric keeps lifecycle events stable; only the fabric-api version pin changes per MC version. |

## Findings

1. **The coupling surface is already narrow**: ~10 distinct vanilla APIs across 6 modules + 2 mixins. Everything user-facing goes through `HudModule`/`ModuleManager`, so most modules touch only STABLE accessors.
2. **The two mixins are the port hotspots** — exactly as designed; they are the whole MIXIN class.
3. **`GuiGraphics` is the one WATCH item used widely.** When P6-02 (adapter layer) happens, wrap the four drawing calls (`drawString`, `fill`, `drawCenteredString`, text metrics) behind a `HudCanvas` interface in `core/compat/` — that single adapter would insulate every HUD module from render-stack churn. That is the only adapter P6-02 needs today; adapters for STABLE rows would be speculative bloat.
4. `util/Keys` and the settings screen are single files by design — a port touches one place each.

**P6-02 recommendation:** implement only the `HudCanvas` adapter (small, high-value); defer everything else until a real second-version port (P6-05) shows actual breakage.
