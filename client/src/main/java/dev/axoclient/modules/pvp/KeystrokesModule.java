package dev.axoclient.modules.pvp;

import dev.axoclient.core.ModuleCategory;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * WASD + mouse-button display (roadmap P1-09). Pure input visualization —
 * firmly on the fair-play side of the feature line (risk R6).
 */
public final class KeystrokesModule extends HudModule {
    private static final int CELL = 18;
    private static final int GAP = 2;
    private static final int PRESSED_BG = 0xE038BDF8;
    private static final int IDLE_BG = 0x90101018;
    private static final int PRESSED_TEXT = 0xFF0A0A0F;
    private static final int IDLE_TEXT = 0xFFE7E9EE;

    public KeystrokesModule() {
        super("keystrokes", "Keystrokes", ModuleCategory.PVP, new HudPosition(HudAnchor.MIDDLE_LEFT, 8, 0));
    }

    @Override
    public void renderHud(GuiGraphics graphics) {
        Options options = Minecraft.getInstance().options;
        int width = CELL * 3 + GAP * 2;
        int height = CELL * 3 + GAP * 2;
        int x = position().anchor().x(graphics.guiWidth(), width, position().offsetX());
        int y = position().anchor().y(graphics.guiHeight(), height, position().offsetY());

        drawKey(graphics, x + CELL + GAP, y, CELL, "W", options.keyUp.isDown());
        drawKey(graphics, x, y + CELL + GAP, CELL, "A", options.keyLeft.isDown());
        drawKey(graphics, x + CELL + GAP, y + CELL + GAP, CELL, "S", options.keyDown.isDown());
        drawKey(graphics, x + (CELL + GAP) * 2, y + CELL + GAP, CELL, "D", options.keyRight.isDown());

        int mouseWidth = (width - GAP) / 2;
        int mouseY = y + (CELL + GAP) * 2;
        drawKey(graphics, x, mouseY, mouseWidth, "LMB", options.keyAttack.isDown());
        drawKey(graphics, x + mouseWidth + GAP, mouseY, mouseWidth, "RMB", options.keyUse.isDown());
    }

    private void drawKey(GuiGraphics graphics, int x, int y, int width, String label, boolean down) {
        graphics.fill(x, y, x + width, y + CELL, down ? PRESSED_BG : IDLE_BG);
        Font font = Minecraft.getInstance().font;
        int textX = x + (width - font.width(label)) / 2;
        int textY = y + (CELL - font.lineHeight) / 2 + 1;
        graphics.drawString(font, label, textX, textY, down ? PRESSED_TEXT : IDLE_TEXT, false);
    }

    @Override
    protected String hudText() {
        return null; // fully custom rendering above
    }
}
