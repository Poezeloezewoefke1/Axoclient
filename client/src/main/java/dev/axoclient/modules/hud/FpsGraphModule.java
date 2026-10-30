package dev.axoclient.modules.hud;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.HudRenderable;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.gui.render.GuiRender;
import dev.axoclient.gui.theme.Themes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A small graph of recent frame times, so a stutter shows as a spike rather
 * than vanishing into an FPS number that averages it away. Bars are coloured
 * by frame time: accent under 60 fps, amber under 30, red below that.
 *
 * Samples are taken in renderHud, which runs once per frame.
 */
public final class FpsGraphModule extends AxoModule implements HudRenderable {
    private static final int SAMPLES = 100;
    private static final int HEIGHT = 30;
    /** Frame time that fills the graph's full height. */
    private static final double CEILING_MS = 50.0;
    private static final double SMOOTH_MS = 1000.0 / 60.0;
    private static final double OK_MS = 1000.0 / 30.0;

    private final double[] frameMs = new double[SAMPLES];
    private int next;
    private long lastFrameNanos;

    public FpsGraphModule() {
        super("fps_graph", "FPS Graph", ModuleCategory.PERFORMANCE, false);
    }

    @Override
    protected void onEnable() {
        java.util.Arrays.fill(frameMs, 0.0);
        lastFrameNanos = 0;
    }

    @Override
    public void renderHud(GuiGraphics graphics) {
        long now = System.nanoTime();
        if (lastFrameNanos != 0) {
            frameMs[next] = (now - lastFrameNanos) / 1_000_000.0;
            next = (next + 1) % SAMPLES;
        }
        lastFrameNanos = now;

        Font font = Minecraft.getInstance().font;
        int accent = Themes.current().accent;
        int left = graphics.guiWidth() - SAMPLES - 6;
        int bottom = graphics.guiHeight() - 6;
        GuiRender.rect(graphics, left - 1, bottom - HEIGHT - 1, SAMPLES + 2, HEIGHT + 2, 0x66000000);
        for (int i = 0; i < SAMPLES; i++) {
            double ms = frameMs[(next + i) % SAMPLES];
            if (ms <= 0) {
                continue;
            }
            int bar = (int) Math.max(1, Math.min(HEIGHT, Math.round(ms / CEILING_MS * HEIGHT)));
            int colour = ms <= SMOOTH_MS ? accent : ms <= OK_MS ? 0xFFF5A524 : 0xFFEF4444;
            GuiRender.rect(graphics, left + i, bottom - bar, 1, bar, colour);
        }
        String label = Minecraft.getInstance().getFps() + " fps";
        graphics.drawString(font, label, left, bottom - HEIGHT - font.lineHeight - 2, accent, true);
    }
}
