package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Player position + facing on the HUD (roadmap P1-08), anchor-positioned. */
public final class CoordinatesModule extends HudModule {

    public CoordinatesModule() {
        super("coordinates", "Coordinates", new HudPosition(HudAnchor.TOP_LEFT, 4, 16));
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        return String.format(
            Locale.ROOT,
            "%.1f / %.1f / %.1f  %s",
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getDirection().toString().toUpperCase(Locale.ROOT)
        );
    }
}
