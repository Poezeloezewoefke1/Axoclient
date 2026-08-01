# 3D halo and wings — findings and plan

Halo and Wings are particle effects. Making them real geometry is the one
outstanding item from the 0.3.0 feedback round. This is what is already
known, so the next attempt does not rediscover it.

## Why it was not done blind

Two things have to be true to write world-space rendering safely, and in the
current sandbox neither is:

1. **The Fabric render API cannot be inspected here.** `maven.fabricmc.net` is
   refused by the environment's network policy (`403` to `CONNECT`), so the
   `fabric-rendering-v1` jar cannot be downloaded and read. The exact shape of
   `WorldRenderContext` in `16.2.10+0290ad933e` is therefore unverified.
2. **Gradle cannot resolve `fabric-loom` locally** for the same reason, so
   there is no local compile. CI is the only compiler available, and CI can
   only ever answer "it compiles" — never "it renders correctly".

Graphics code that compiles but is inverted, mis-scaled, mis-positioned or
z-fighting looks identical to working code from CI's point of view. Shipping
that as done would be a guess wearing a green tick.

## The specific unknowns to resolve first

- Does `WorldRenderContext.matrixStack()` still exist and return non-null in
  this Fabric version? Mojang moved world rendering away from `PoseStack` in
  the 1.21.x line, and Fabric followed; this is the single biggest risk.
- Vertex builder method names: `addVertex(...)`/`setColor(...)` (current) vs
  `vertex(...)`/`color(...)` (older).
- Which `RenderType` to use without shipping a texture.
  `RenderType.debugFilledBox()` is `POSITION_COLOR` in `TRIANGLE_STRIP` mode,
  which suits a ring band and needs no asset.

Resolve these by reading the actual jar, not from memory.

## Approach when unblocked

Render from `WorldRenderEvents.AFTER_ENTITIES` rather than a player-renderer
mixin: no injection point to break on a Minecraft update, and it composes with
the existing module toggles.

- **Halo** — a flat annulus as a triangle strip: step the angle, emitting the
  outer then the inner vertex at each step, closing the loop. A real ring with
  a hole, not a sprite.
- **Wings** — two swept triangle strips off the shoulders, built from the
  player's yaw so they turn with the body, with a slow flap driven by tick
  count plus partial ticks.

Both should read the same `radius` / `span` / `points` settings the particle
versions already expose, so switching implementation does not reset anyone's
configuration.

## Non-negotiable safety guard

Wrap the whole render body in `catch (Throwable)` and latch a flag that
permanently skips rendering after the first failure, logging once. World
rendering runs every frame; an unguarded throw there is a hard crash, and this
project has already shipped one boot crash to players. Degrading to "the
cosmetic does not draw" matches how the optional mixins already fail.

## Testing that actually proves it

CI green is necessary and not sufficient. It needs eyes on a frame:
first person and third person, moving and turning, in daylight and in a dark
cave, with the module toggled off and back on.
