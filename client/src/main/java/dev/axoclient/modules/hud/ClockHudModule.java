package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * The in-game time of day as a 24-hour clock. Tick 0 is sunrise at 06:00
 * and a day is 24000 ticks, so every 1000 ticks is one in-game hour.
 *
 * This id used to be a second copy of the real-world Clock (time_hud); it was
 * repurposed rather than removed so existing configs keep loading cleanly.
 */
public final class ClockHudModule extends HudModule {
    private static final long DAY_TICKS = 24000L;

    public ClockHudModule() {
        super("clock_hud", "Game Clock", new HudPosition(HudAnchor.TOP_RIGHT, -4, 28), false);
    }

    @Override
    protected String hudText() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        long time = Math.floorMod(level.getDayTime(), DAY_TICKS);
        long hours = (time / 1000 + 6) % 24;
        long minutes = (time % 1000) * 60 / 1000;
        return String.format(Locale.ROOT, "Game %02d:%02d", hours, minutes);
    }
}
