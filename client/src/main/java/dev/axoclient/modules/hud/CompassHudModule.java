package dev.axoclient.modules.hud;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.HudRenderable;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.gui.theme.Themes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

/**
 * A sliding compass strip across the top of the screen, marking the four
 * cardinal directions as you turn. Draws itself rather than extending
 * HudModule because it's a graphic, not a line of text.
 */
public final class CompassHudModule extends AxoModule implements HudRenderable {
    private static final String[] MARKS = {"S", "W", "N", "E"};
    /** Screen pixels per degree of yaw. */
    private static final float PIXELS_PER_DEGREE = 1.6F;
    private static final int WIDTH = 180;

    public CompassHudModule() {
        super("compass_hud", "Compass", ModuleCategory.HUD, false);
    }

    @Override
    public void renderHud(GuiGraphics graphics) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int accent = Themes.current().accent;
        int centerX = graphics.guiWidth() / 2;
        int left = centerX - WIDTH / 2;
        int top = 4;
        int height = font.lineHeight + 4;

        graphics.fill(left, top, left + WIDTH, top + height, 0x66000000);

        float yaw = player.getYRot();
        for (int i = 0; i < MARKS.length; i++) {
            // Yaw 0 faces south; each mark sits 90° apart from there.
            float markYaw = i * 90.0F;
            float delta = wrapDegrees(markYaw - yaw);
            int x = centerX + Math.round(delta * PIXELS_PER_DEGREE);
            if (x < left + 4 || x > left + WIDTH - 4) {
                continue;
            }
            String mark = MARKS[i];
            graphics.drawString(font, mark, x - font.width(mark) / 2, top + 2, accent, true);
        }
        // Fixed centre needle showing where you're actually facing.
        graphics.fill(centerX, top, centerX + 1, top + height, accent);
    }

    /** Normalises a degree difference into -180..180. */
    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }
}
