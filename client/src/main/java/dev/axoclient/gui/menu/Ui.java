package dev.axoclient.gui.menu;

import dev.axoclient.gui.theme.Palette;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * Drawing for the client menu in design pixels.
 *
 * The menu is laid out on the design's own 1280x720 canvas and scaled to the
 * window by {@link ClientMenuScreen}, so every number here is the number from
 * the design file. Text uses the design's fonts (Inter and Instrument Serif)
 * shipped as TrueType fonts under assets/axoclient/font, one definition per
 * size and weight the design uses.
 */
final class Ui {
    /** One font face at one size: "S14" is Inter SemiBold (600) at 14px. */
    enum Type {
        R10, R11, R12, R13,
        M12, M13, M14,
        S9, S10, S11, S13, S14, S15, S16, S17, S26,
        B9, B10, B11, B12,
        SERIF19, SERIF30;

        final int size;
        final boolean serif;
        private final Style style;

        Type() {
            String n = name();
            this.serif = n.startsWith("SERIF");
            this.size = Integer.parseInt(serif ? n.substring(5) : n.substring(1));
            String file = serif ? "menu_serif" + size : "menu_" + Character.toLowerCase(n.charAt(0)) + size;
            this.style = Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("axoclient", file)));
        }

        MutableComponent text(String s) {
            return Component.literal(s).withStyle(style);
        }

        /** Distance from the top of a CSS line box (line-height: normal) to the baseline. */
        float ascent() {
            return size * (serif ? 0.99f : 0.969f);
        }

        float capHeight() {
            return size * (serif ? 0.72f : 0.728f);
        }
    }

    /**
     * Where Minecraft puts a TrueType font's baseline below the y it is drawn
     * at, in font pixels. Every text helper goes through this one number, so
     * if text sits uniformly high or low in game, this is the fix.
     */
    static final float BASELINE = 7f;

    private static final Identifier ICONS = Identifier.fromNamespaceAndPath("axoclient", "textures/gui/menu_icons.png");
    private static final int ICON_CELL = 64;
    private static final int ICON_COLS = 8;
    private static final int ICON_SHEET_W = 512;
    private static final int ICON_SHEET_H = 384;

    private Ui() {
    }

    static Font font() {
        return Minecraft.getInstance().font;
    }

    // ------------------------------------------------------------------
    // Shapes
    // ------------------------------------------------------------------

    static void rect(GuiGraphics g, int x, int y, int w, int h, int c) {
        if (w > 0 && h > 0) {
            g.fill(x, y, x + w, y + h, c);
        }
    }

    /** A filled rounded rectangle, corners drawn one row at a time from the circle. */
    static void round(GuiGraphics g, int x, int y, int w, int h, int r, int c) {
        if (w <= 0 || h <= 0 || (c >>> 24) == 0) {
            return;
        }
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, c);
            g.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, c);
        }
        g.fill(x, y + r, x + w, y + h - r, c);
    }

    /** A rounded box with a {@code bw}-pixel border, as CSS border + border-radius. */
    static void box(GuiGraphics g, int x, int y, int w, int h, int r, int fill, int border, int bw) {
        // CSS paints the background under the border, so a translucent border
        // tints the fill at the edge: fill, then border, then fill again inside.
        if ((fill >>> 24) == 0xFF) {
            round(g, x, y, w, h, r, fill);
        }
        round(g, x, y, w, h, r, border);
        round(g, x + bw, y + bw, w - bw * 2, h - bw * 2, Math.max(0, r - bw), fill);
    }

    static void box(GuiGraphics g, int x, int y, int w, int h, int r, int fill, int border) {
        box(g, x, y, w, h, r, fill, border, 1);
    }

    /** A CSS-like drop shadow: stacked, fading rounded rectangles under a box. */
    static void shadow(GuiGraphics g, int x, int y, int w, int h, int r, int spread, int offsetY, int alpha) {
        int steps = 6;
        for (int i = steps; i >= 1; i--) {
            int grow = spread * i / steps;
            int a = alpha * (steps - i + 1) / (steps * 3);
            round(g, x - grow, y - grow + offsetY, w + grow * 2, h + grow * 2, r + grow, a << 24);
        }
    }

    /** The design's switch: a pill track and a white knob with 3px padding. */
    static void toggle(GuiGraphics g, int x, int y, int w, int h, boolean on, Palette p) {
        round(g, x, y, w, h, h / 2, on ? p.accent() : p.off());
        int knob = h - 6;
        round(g, on ? x + w - 3 - knob : x + 3, y + 3, knob, knob, knob / 2, 0xFFFFFFFF);
    }

    /** One of the design's line icons, tinted, drawn at {@code size} pixels. */
    static void icon(GuiGraphics g, MenuIcon icon, int x, int y, int size, int colour) {
        int index = icon.ordinal();
        float u = (index % ICON_COLS) * ICON_CELL;
        float v = (index / ICON_COLS) * ICON_CELL;
        g.blit(RenderPipelines.GUI_TEXTURED, ICONS, x, y, u, v, size, size, ICON_CELL, ICON_CELL,
            ICON_SHEET_W, ICON_SHEET_H, colour);
    }

    // ------------------------------------------------------------------
    // Text
    // ------------------------------------------------------------------

    static int width(Type t, String s) {
        return font().width(t.text(s));
    }

    private static void at(GuiGraphics g, Type t, String s, int x, float baseline, int colour) {
        g.drawString(font(), t.text(s), x, Math.round(baseline - BASELINE), colour, false);
    }

    /** Text whose CSS line box starts at {@code top}. */
    static void text(GuiGraphics g, Type t, String s, int x, int top, int colour) {
        at(g, t, s, x, top + t.ascent(), colour);
    }

    /** Text vertically centred on {@code midY} (by cap height, as flex centring reads). */
    static void mid(GuiGraphics g, Type t, String s, int x, int midY, int colour) {
        at(g, t, s, x, midY + t.capHeight() / 2f, colour);
    }

    static void midCentered(GuiGraphics g, Type t, String s, int cx, int midY, int colour) {
        mid(g, t, s, cx - width(t, s) / 2, midY, colour);
    }

    static void midRight(GuiGraphics g, Type t, String s, int right, int midY, int colour) {
        mid(g, t, s, right - width(t, s), midY, colour);
    }

    /** CSS letter-spacing: each character drawn on its own with {@code em} extra space. */
    static void spaced(GuiGraphics g, Type t, String s, int x, int midY, int colour, float em) {
        float cx = x;
        float baseline = midY + t.capHeight() / 2f;
        for (int i = 0; i < s.length(); i++) {
            String ch = s.substring(i, i + 1);
            g.drawString(font(), t.text(ch), Math.round(cx), Math.round(baseline - BASELINE), colour, false);
            cx += width(t, ch) + t.size * em;
        }
    }

    static int spacedWidth(Type t, String s, float em) {
        if (s.isEmpty()) {
            return 0;
        }
        return width(t, s) + Math.round(t.size * em * (s.length() - 1));
    }

    static void spacedCentered(GuiGraphics g, Type t, String s, int cx, int midY, int colour, float em) {
        spaced(g, t, s, cx - spacedWidth(t, s, em) / 2, midY, colour, em);
    }

    static String trim(Type t, String s, int maxWidth) {
        if (width(t, s) <= maxWidth) {
            return s;
        }
        String cut = s;
        while (cut.length() > 1 && width(t, cut + "…") > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "…";
    }

    static List<String> wrap(Type t, String s, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : s.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && width(t, candidate) > maxWidth) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    // ------------------------------------------------------------------
    // Controls shared by several pages
    // ------------------------------------------------------------------

    /**
     * The design's segmented "mode" control: a raised strip with 3px padding,
     * the selected option filled in the accent. Returns option edges for hits.
     */
    static int[] segmented(GuiGraphics g, Palette p, int right, int y, int h, List<String> options, int selected,
                           Type t, int pad, int radius) {
        int total = segmentedWidth(t, options, pad);
        int x = right - total;
        round(g, x, y, total, h, radius + 2, p.raised());
        int[] edges = new int[options.size() + 1];
        int cx = x + 3;
        for (int i = 0; i < options.size(); i++) {
            int w = width(t, options.get(i)) + pad * 2;
            edges[i] = cx;
            if (i == selected) {
                round(g, cx, y + 3, w, h - 6, radius, p.accent());
            }
            midCentered(g, t, options.get(i), cx + w / 2, y + h / 2, i == selected ? p.onAccent() : p.sub());
            cx += w + 3;
        }
        edges[options.size()] = cx;
        return edges;
    }

    static int segmentedWidth(Type t, List<String> options, int pad) {
        int total = 3;
        for (String o : options) {
            total += width(t, o) + pad * 2 + 3;
        }
        return total;
    }

    /** A filter chip (height 30, padding 0 14, radius 8, 11px semibold, letter-spaced). */
    static int chip(GuiGraphics g, Palette p, String label, int x, int y, boolean active, int padX) {
        int w = spacedWidth(Type.S11, label, 0.12f) + padX * 2;
        box(g, x, y, w, 30, 8, active ? p.accent() : p.raised(), active ? p.accent() : p.border());
        spacedCentered(g, Type.S11, label, x + w / 2, y + 15, active ? p.onAccent() : p.sub(), 0.12f);
        return w;
    }

    static int chipWidth(String label, int padX) {
        return spacedWidth(Type.S11, label, 0.12f) + padX * 2;
    }
}
