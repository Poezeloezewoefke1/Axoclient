package dev.axoclient.modules.hud;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.HudRenderable;
import dev.axoclient.core.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Proof-of-life for the module system: draws the FPS counter on the HUD.
 * Position is fixed top-left until the anchor system lands (roadmap P1-06).
 */
public final class FpsHudModule extends AxoModule implements HudRenderable {
    private static final int AXO_BLUE = 0xFF38BDF8;
    private static final int MARGIN = 4;

    public FpsHudModule() {
        super("fps_hud", "FPS Counter", ModuleCategory.HUD, true);
    }

    @Override
    public void renderHud(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        String text = minecraft.getFps() + " fps";
        graphics.drawString(minecraft.font, text, MARGIN, MARGIN, AXO_BLUE, true);
    }
}
