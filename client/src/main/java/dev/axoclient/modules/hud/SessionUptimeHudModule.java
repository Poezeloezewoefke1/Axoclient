package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.Locale;

/** How long this game session has been running (mm:ss). Opt-in. */
public final class SessionUptimeHudModule extends HudModule {
    private static final long START = System.currentTimeMillis();

    public SessionUptimeHudModule() {
        super("uptime_hud", "Session Time", new HudPosition(HudAnchor.BOTTOM_LEFT, 4, -14), false);
    }

    @Override
    protected String hudText() {
        long seconds = (System.currentTimeMillis() - START) / 1000L;
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }
}
