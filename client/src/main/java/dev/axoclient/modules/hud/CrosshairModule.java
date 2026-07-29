package dev.axoclient.modules.hud;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.HudRenderable;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.core.ModuleSetting;
import dev.axoclient.gui.render.GuiRender;
import dev.axoclient.gui.theme.Themes;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * An accent-coloured crosshair drawn on top of the vanilla one.
 *
 * It draws over rather than replacing: swapping the vanilla crosshair out
 * would need a render mixin, and a failed mixin stops the game from starting
 * at all. Overdrawing gets the same look for none of that risk — pick the
 * "dot" style and the result reads as a clean custom crosshair.
 *
 * Config (module "crosshair"):
 *   style — 0 dot, 1 cross, 2 circle
 *   size  — arm length / radius in pixels
 *   gap   — pixels of empty space at the centre (cross only)
 */
public final class CrosshairModule extends AxoModule implements HudRenderable {
    public static final int STYLE_DOT = 0;
    public static final int STYLE_CROSS = 1;
    public static final int STYLE_CIRCLE = 2;

    public CrosshairModule() {
        super("crosshair", "Crosshair", ModuleCategory.HUD, false);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(
            ModuleSetting.plain(id(), "style", "Style (0 dot, 1 cross, 2 ring)", 0, 2, STYLE_DOT),
            ModuleSetting.plain(id(), "size", "Size", 1, 12, 4),
            ModuleSetting.plain(id(), "gap", "Centre gap", 0, 8, 2)
        );
    }

    @Override
    public void renderHud(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        int cx = graphics.guiWidth() / 2;
        int cy = graphics.guiHeight() / 2;
        int colour = Themes.current().accent;
        int style = ModuleManager.get().config().getModuleInt(id(), "style", STYLE_DOT);
        int size = clamp(ModuleManager.get().config().getModuleInt(id(), "size", 4), 1, 12);
        int gap = clamp(ModuleManager.get().config().getModuleInt(id(), "gap", 2), 0, 8);

        switch (style) {
            case STYLE_CROSS -> drawCross(graphics, cx, cy, size, gap, colour);
            case STYLE_CIRCLE -> drawCircle(graphics, cx, cy, size, colour);
            default -> GuiRender.rect(graphics, cx - 1, cy - 1, 2, 2, colour);
        }
    }

    private static void drawCross(GuiGraphics graphics, int cx, int cy, int size, int gap, int colour) {
        GuiRender.rect(graphics, cx - gap - size, cy, size, 1, colour);
        GuiRender.rect(graphics, cx + gap + 1, cy, size, 1, colour);
        GuiRender.rect(graphics, cx, cy - gap - size, 1, size, colour);
        GuiRender.rect(graphics, cx, cy + gap + 1, 1, size, colour);
    }

    /** Midpoint-ish circle: cheap, and at these radii nobody counts the pixels. */
    private static void drawCircle(GuiGraphics graphics, int cx, int cy, int radius, int colour) {
        int steps = Math.max(12, radius * 6);
        for (int i = 0; i < steps; i++) {
            double angle = (Math.PI * 2 * i) / steps;
            int x = cx + (int) Math.round(Math.cos(angle) * radius);
            int y = cy + (int) Math.round(Math.sin(angle) * radius);
            GuiRender.rect(graphics, x, y, 1, 1, colour);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
