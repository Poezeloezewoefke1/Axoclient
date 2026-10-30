package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import dev.axoclient.util.TimeFormat;
import dev.axoclient.util.WorldInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Real-time countdown to the next nightfall or sunrise, so you know whether
 * there's time for one more trip before the monsters come out.
 *
 * A day is 24000 ticks. Night is counted from 13000, about when hostile
 * mobs can start spawning on the surface, to 23000, when the sun is up far
 * enough to burn them. Only shown in the Overworld; elsewhere the sky
 * doesn't cycle.
 */
public final class DayCycleModule extends HudModule {
    private static final long DAY_TICKS = 24000L;
    private static final long NIGHT_START = 13000L;
    private static final long NIGHT_END = 23000L;

    public DayCycleModule() {
        super("day_cycle", "Day Cycle", new HudPosition(HudAnchor.TOP_RIGHT, -4, 100), false);
    }

    @Override
    protected String hudText() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !WorldInfo.OVERWORLD.equals(WorldInfo.dimensionId())) {
            return null;
        }
        long time = Math.floorMod(level.getDayTime(), DAY_TICKS);
        boolean night = time >= NIGHT_START && time < NIGHT_END;
        long ticksLeft = night ? NIGHT_END - time : Math.floorMod(NIGHT_START - time, DAY_TICKS);
        // 20 ticks a second when the server keeps up.
        String left = TimeFormat.clock(ticksLeft * 50L);
        return night ? "Sunrise in " + left : "Night in " + left;
    }
}
