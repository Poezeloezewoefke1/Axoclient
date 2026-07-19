package dev.axoclient.modules.hud;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.HudRenderable;
import dev.axoclient.core.ModuleCategory;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

/**
 * Player position + facing on the HUD (roadmap P1-08). Fixed position under
 * the FPS counter until the anchor system lands (P1-06).
 */
public final class CoordinatesModule extends AxoModule implements HudRenderable {
    private static final int AXO_BLUE = 0xFF38BDF8;

    public CoordinatesModule() {
        super("coordinates", "Coordinates", ModuleCategory.HUD, true);
    }

    @Override
    public void renderHud(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        String text = String.format(
            Locale.ROOT,
            "%.1f / %.1f / %.1f  %s",
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getDirection().toString().toUpperCase(Locale.ROOT)
        );
        graphics.drawString(minecraft.font, text, 4, 16, AXO_BLUE, true);
    }
}
