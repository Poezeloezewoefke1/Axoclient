package dev.axoclient.hud;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.HudRenderable;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Base class for single-line HUD text modules. Handles anchored
 * positioning (P1-06); subclasses only supply the text. The position is
 * re-read from config on every enable, so config edits apply on toggle
 * or restart (live drag-editing is P5-09).
 */
public abstract class HudModule extends AxoModule implements HudRenderable {
    protected static final int AXO_BLUE = 0xFF38BDF8;

    private final HudPosition defaultPosition;
    private HudPosition position;

    protected HudModule(String id, String displayName, HudPosition defaultPosition) {
        super(id, displayName, ModuleCategory.HUD, true);
        this.defaultPosition = defaultPosition;
        this.position = defaultPosition;
    }

    /** The line to draw this frame, or null to draw nothing. */
    protected abstract String hudText();

    @Override
    protected void onEnable() {
        position = HudPosition.load(ModuleManager.get().config(), id(), defaultPosition);
    }

    @Override
    public final void renderHud(GuiGraphics graphics) {
        String text = hudText();
        if (text == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        int x = position.anchor().x(graphics.guiWidth(), minecraft.font.width(text), position.offsetX());
        int y = position.anchor().y(graphics.guiHeight(), minecraft.font.lineHeight, position.offsetY());
        graphics.drawString(minecraft.font, text, x, y, AXO_BLUE, true);
    }
}
