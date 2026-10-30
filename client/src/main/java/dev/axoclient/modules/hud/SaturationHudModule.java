package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * The hidden food stat. Hunger only starts dropping once saturation is used
 * up, and natural regeneration is fastest while it's above zero, so this is
 * the number that says when you actually need to eat.
 */
public final class SaturationHudModule extends HudModule {

    public SaturationHudModule() {
        super("saturation_hud", "Saturation", new HudPosition(HudAnchor.TOP_LEFT, 4, 184), false);
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        return String.format(Locale.ROOT, "Saturation %.1f", player.getFoodData().getSaturationLevel());
    }
}
