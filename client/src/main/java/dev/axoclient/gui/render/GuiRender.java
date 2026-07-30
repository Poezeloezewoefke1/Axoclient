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

    /**
     * A themed panel: drop shadow, body, accent top bar, outline, and corners
     * bitten out so it reads as rounded.
     */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, GuiTheme theme) {
        shadow(g, x, y, w, h);
        rect(g, x, y, w, h, theme.panel);
        // Vertical lift: slightly brighter at the top, so panels have a light
        // source instead of looking like flat cutouts.
        verticalGradient(g, x + 1, y + 2, w - 2, Math.min(18, h - 3), 0x14FFFFFF, 0x00FFFFFF);
        rect(g, x, y, w, 2, theme.accent);
        outline(g, x, y, w, h, theme.outline);
        roundCorners(g, x, y, w, h);
    }

    /**
     * Soft shadow below and right of a box, drawn as three fading rings.
     *
     * fill() cannot blur, so this approximates one. It is cheap and, at these
     * alphas, indistinguishable from a real shadow at GUI scale.
     */
    public static void shadow(GuiGraphics g, int x, int y, int w, int h) {
        for (int i = 3; i >= 1; i--) {
            int alpha = 0x18 - (i * 6);
            if (alpha <= 0) {
                continue;
            }
            rect(g, x - i, y - i + 2, w + i * 2, h + i * 2, alpha << 24);
        }
    }

    /**
     * Punch the four corner pixels to transparent-black so a square panel
     * reads as rounded. Two pixels per corner is the most that looks right at
     * GUI scale; more starts to look chewed.
     */
    public static void roundCorners(GuiGraphics g, int x, int y, int w, int h) {
        int c = 0x00000000;
        // Only the single outermost pixel of each corner, cleared.
        g.fill(x, y, x + 1, y + 1, c);
        g.fill(x + w - 1, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + 1, y + h, c);
        g.fill(x + w - 1, y + h - 1, x + w, y + h, c);
    }

    /**
     * Top-to-bottom gradient, one scanline at a time.
     *
     * GuiGraphics has a gradient helper, but its name and signature have moved
     * between versions; a loop over fill() is a few more pixels of work and
     * cannot break on a port.
     */
    public static void verticalGradient(GuiGraphics g, int x, int y, int w, int h, int top, int bottom) {
        if (h <= 0 || w <= 0) {
            return;
        }
        for (int i = 0; i < h; i++) {
            float t = h == 1 ? 0f : i / (float) (h - 1);
            g.fill(x, y + i, x + w, y + i + 1, blend(top, bottom, t));
        }
    }

    /** A horizontal accent bar with a soft falloff — used for section headers. */
    public static void accentUnderline(GuiGraphics g, int x, int y, int w, int argb) {
        rect(g, x, y, w, 1, argb);
        rect(g, x, y + 1, w, 1, (argb & 0x00FFFFFF) | 0x40000000);
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
