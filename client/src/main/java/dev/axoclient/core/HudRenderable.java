package dev.axoclient.core;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Implemented by modules that draw on the in-game HUD. Rendering is
 * dispatched from the Gui render mixin via {@link ModuleManager#renderHud}.
 */
public interface HudRenderable {
    void renderHud(GuiGraphics graphics);
}
