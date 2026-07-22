package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;

/** In-world day number, derived from the level's day time. Opt-in. */
public final class DayCounterHudModule extends HudModule {

    public DayCounterHudModule() {
        super("day_counter", "Day Counter", new HudPosition(HudAnchor.TOP_LEFT, 4, 64), false);
    }

    @Override
    protected String hudText() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        return "Day " + (minecraft.level.getDayTime() / 24000L);
    }
}
