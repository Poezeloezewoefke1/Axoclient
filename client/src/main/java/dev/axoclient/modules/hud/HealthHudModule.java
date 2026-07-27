package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Current / max health on the HUD — handy when hearts are hidden or busy. */
public final class HealthHudModule extends HudModule {

    public HealthHudModule() {
        super("health_hud", "Health", new HudPosition(HudAnchor.TOP_LEFT, 4, 52), false);
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        return String.format(Locale.ROOT, "HP %.0f/%.0f", player.getHealth(), player.getMaxHealth());
    }
}
