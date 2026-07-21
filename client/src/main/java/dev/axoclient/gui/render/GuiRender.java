package dev.axoclient.gui.render;

import dev.axoclient.gui.theme.GuiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Flat, modern draw helpers built on GuiGraphics#fill so they compile
 * safely across mappings (no custom buffers). Panels get a 1px accent top
 * bar and a subtle outline, which reads as clean and modern without needing
 * real rounded corners.
 */
public final class GuiRender {
    private GuiRender() {
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    public static void rect(GuiGraphics g, int x, int y, int w, int h, int argb) {
        g.fill(x, y, x + w, y + h, argb);
    }

    public static void outline(GuiGraphics g, int x, int y, int w, int h, int argb) {
        g.fill(x, y, x + w, y + 1, argb);
        g.fill(x, y + h - 1, x + w, y + h, argb);
        g.fill(x, y, x + 1, y + h, argb);
        g.fill(x + w - 1, y, x + w, y + h, argb);
    }

    /** A themed panel: body, accent top bar, outline. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, GuiTheme theme) {
        rect(g, x, y, w, h, theme.panel);
        rect(g, x, y, w, 2, theme.accent);
        outline(g, x, y, w, h, theme.outline);
    }

    public static void text(GuiGraphics g, String s, int x, int y, int argb) {
        g.drawString(font(), s, x, y, argb, false);
    }

    public static void textShadow(GuiGraphics g, String s, int x, int y, int argb) {
        g.drawString(font(), s, x, y, argb, true);
    }

    public static void centered(GuiGraphics g, String s, int cx, int y, int argb) {
        g.drawString(font(), s, cx - font().width(s) / 2, y, argb, false);
    }

    public static int textWidth(String s) {
        return font().width(s);
    }

    public static int lineHeight() {
        return font().lineHeight;
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    /** Blend two ARGB colours by t in [0,1] — used for hover/toggle transitions. */
    public static int blend(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = lerpChannel(from >>> 24, to >>> 24, t);
        int r = lerpChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int gg = lerpChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = lerpChannel(from & 0xFF, to & 0xFF, t);
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }

    private static int lerpChannel(int from, int to, float t) {
        return Math.round(from + (to - from) * t) & 0xFF;
    }
}
