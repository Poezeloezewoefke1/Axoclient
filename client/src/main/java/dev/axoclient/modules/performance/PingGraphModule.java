package dev.axoclient.modules.performance;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.HudRenderable;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.gui.render.GuiRender;
import dev.axoclient.gui.theme.Themes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

/**
 * Your ping over the last minute as a small bar graph, so you can see whether
 * that rubber-banding was a spike or the connection has been bad all along.
 * Sits just above the FPS graph's spot so both can be on together.
 *
 * Sampled once a second from the tab-list latency, which the server updates
 * periodically — the same number the Ping module shows. Hidden in
 * singleplayer, where it's always zero.
 */
public final class PingGraphModule extends AxoModule implements HudRenderable {
    private static final int SAMPLES = 60;
    private static final int HEIGHT = 24;
    private static final int SAMPLE_TICKS = 20;
    /** Ping that fills the graph's height; anything above is clipped. */
    private static final int CEILING_MS = 300;
    private static final int GOOD_MS = 80;
    private static final int OK_MS = 150;
    /** Bottom of the FPS graph's area plus its height and label. */
    private static final int ABOVE_FPS_GRAPH = 6 + 30 + 12 + 6;

    private final int[] samples = new int[SAMPLES];
    private int next;
    private int count;
    private int ticks;

    public PingGraphModule() {
        super("ping_graph", "Ping Graph", ModuleCategory.PERFORMANCE, false);
    }

    @Override
    protected void onEnable() {
        count = 0;
        next = 0;
    }

    @Override
    public void onTick() {
        if (++ticks < SAMPLE_TICKS) {
            return;
        }
        ticks = 0;
        int ping = currentPing();
        if (ping < 0) {
            count = 0;
            return;
        }
        samples[next] = ping;
        next = (next + 1) % SAMPLES;
        count = Math.min(SAMPLES, count + 1);
    }

    private static int currentPing() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (minecraft.player == null || connection == null || minecraft.getCurrentServer() == null) {
            return -1;
        }
        PlayerInfo info = connection.getPlayerInfo(minecraft.player.getUUID());
        return info == null ? -1 : info.getLatency();
    }

    @Override
    public void renderHud(GuiGraphics graphics) {
        if (count == 0) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int accent = Themes.current().accent;
        int barW = 2;
        int width = SAMPLES * barW;
        int left = graphics.guiWidth() - width - 6;
        int bottom = graphics.guiHeight() - ABOVE_FPS_GRAPH;
        GuiRender.rect(graphics, left - 1, bottom - HEIGHT - 1, width + 2, HEIGHT + 2, 0x66000000);

        int latest = 0;
        // Oldest on the left: start from the slot after the newest sample.
        for (int i = 0; i < count; i++) {
            int index = Math.floorMod(next - count + i, SAMPLES);
            int ping = samples[index];
            latest = ping;
            int bar = Math.max(1, Math.min(HEIGHT, Math.round(ping / (float) CEILING_MS * HEIGHT)));
            int colour = ping <= GOOD_MS ? accent : ping <= OK_MS ? 0xFFF5A524 : 0xFFEF4444;
            int x = left + (SAMPLES - count + i) * barW;
            GuiRender.rect(graphics, x, bottom - bar, barW - 1, bar, colour);
        }
        graphics.drawString(font, latest + " ms", left, bottom - HEIGHT - font.lineHeight - 2, accent, true);
    }
}
