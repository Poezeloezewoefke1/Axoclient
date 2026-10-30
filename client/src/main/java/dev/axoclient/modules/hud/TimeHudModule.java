package dev.axoclient.modules.hud;

import dev.axoclient.core.ModuleSetting;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Real-world clock on the HUD — handy for knowing the time without alt-tabbing.
 *
 * Config (module "time_hud"):
 *   format — 24-hour or 12-hour
 */
public final class TimeHudModule extends HudModule {
    private static final DateTimeFormatter FORMAT_24 = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FORMAT_12 = DateTimeFormatter.ofPattern("h:mm a", Locale.ROOT);

    public TimeHudModule() {
        super("time_hud", "Clock", new HudPosition(HudAnchor.TOP_RIGHT, -4, 4));
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(ModuleSetting.choice(id(), "format", "Format", List.of("24-hour", "12-hour"), 0));
    }

    @Override
    protected String hudText() {
        return LocalTime.now().format(settings().get(0).get() == 1 ? FORMAT_12 : FORMAT_24);
    }
}
