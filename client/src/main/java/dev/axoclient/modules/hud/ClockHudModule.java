package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Real-world clock on the HUD — check the time without alt-tabbing. */
public final class ClockHudModule extends HudModule {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public ClockHudModule() {
        super("clock_hud", "Clock", new HudPosition(HudAnchor.TOP_RIGHT, -4, 28), false);
    }

    @Override
    protected String hudText() {
        return LocalTime.now().format(FORMAT);
    }
}
