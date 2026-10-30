package dev.axoclient.gui.menu;

import dev.axoclient.gui.theme.Palette;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Drawing for the client menu's look: rounded cards, pill switches,
 * segmented controls and chips, plus the sidebar's small icons.
 *
 * Everything is built from GuiGraphics#fill so it can't break on a port.
 * Rounded corners are drawn one scanline at a time from the circle equation;
 * at GUI scale a radius of 3-6 pixels is all a corner needs.
 */
public final class Draw {
    private Draw() {
    }

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    public static int width(String text) {
        return font().width(text);
    }

    /** A filled rectangle with rounded corners of radius {@code r}. */
    public static void round(GuiGraphics g, int x, int y, int w, int h, int r, int colour) {
        if (w <= 0 || h <= 0) {
            return;
        }
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, colour);
            g.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, colour);
        }
        g.fill(x, y + r, x + w, y + h - r, colour);
    }

    /** A rounded card: a hairline border with an opaque fill inside it. */
    public static void card(GuiGraphics g, int x, int y, int w, int h, int r, int fill, int border) {
        round(g, x, y, w, h, r, border);
        round(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
    }

    /** The design's on/off switch: a pill track with a white knob at one end. */
    public static void toggle(GuiGraphics g, int x, int y, int w, int h, boolean on, Palette p) {
        round(g, x, y, w, h, h / 2, on ? p.accent() : p.off());
        int knob = h - 4;
        int kx = on ? x + w - knob - 2 : x + 2;
        round(g, kx, y + 2, knob, knob, knob / 2, 0xFFFFFFFF);
    }

    /** Text in the design's small uppercase "label" style. */
    public static void label(GuiGraphics g, String text, int x, int y, int colour) {
        g.drawString(font(), text.toUpperCase(java.util.Locale.ROOT), x, y, colour, false);
    }

    public static void text(GuiGraphics g, String text, int x, int y, int colour) {
        g.drawString(font(), text, x, y, colour, false);
    }

    public static void bold(GuiGraphics g, String text, int x, int y, int colour) {
        g.drawString(font(), Component.literal(text).withStyle(ChatFormatting.BOLD), x, y, colour, false);
    }

    public static int boldWidth(String text) {
        return font().width(Component.literal(text).withStyle(ChatFormatting.BOLD));
    }

    public static void centered(GuiGraphics g, String text, int cx, int y, int colour) {
        g.drawString(font(), text, cx - font().width(text) / 2, y, colour, false);
    }

    public static void boldCentered(GuiGraphics g, String text, int cx, int y, int colour) {
        bold(g, text, cx - boldWidth(text) / 2, y, colour);
    }

    /** {@code text} cut to fit {@code maxWidth}, with "..." when it had to be cut. */
    public static String trim(String text, int maxWidth) {
        if (font().width(text) <= maxWidth) {
            return text;
        }
        String cut = text;
        while (cut.length() > 1 && font().width(cut + "...") > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "...";
    }

    /** Word-wraps {@code text} into lines no wider than {@code maxWidth}. */
    public static List<String> wrap(String text, int maxWidth) {
        List<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && width(candidate) > maxWidth) {
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

    /**
     * A row of options with the chosen one filled in the accent — the design's
     * "mode" control. Returns the x where each option starts plus the end, so
     * the caller can hit-test clicks against the same layout.
     */
    public static int[] segmented(
        GuiGraphics g, int x, int y, int h, List<String> options, int selected, Palette p, int pad
    ) {
        int[] edges = new int[options.size() + 1];
        int total = 4;
        for (String option : options) {
            total += width(option) + pad * 2 + 2;
        }
        round(g, x, y, total, h, 5, p.raised());
        int cx = x + 2;
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            int w = width(option) + pad * 2;
            edges[i] = cx;
            if (i == selected) {
                round(g, cx, y + 2, w, h - 4, 4, p.accent());
            }
            centered(g, option, cx + w / 2, y + (h - 8) / 2, i == selected ? p.onAccent() : p.sub());
            cx += w + 2;
        }
        edges[options.size()] = cx;
        return edges;
    }

    /** Total width {@link #segmented} will use for these options. */
    public static int segmentedWidth(List<String> options, int pad) {
        int total = 4;
        for (String option : options) {
            total += width(option) + pad * 2 + 2;
        }
        return total;
    }

    /** A filter chip: accent-filled when active, a raised outline otherwise. Returns its width. */
    public static int chip(GuiGraphics g, String text, int x, int y, int h, boolean active, Palette p) {
        String shown = text.toUpperCase(java.util.Locale.ROOT);
        int w = width(shown) + 16;
        card(g, x, y, w, h, 5, active ? p.accent() : p.raised(), active ? p.accent() : p.border());
        centered(g, shown, x + w / 2, y + (h - 8) / 2, active ? p.onAccent() : p.sub());
        return w;
    }

    public static int chipWidth(String text) {
        return width(text.toUpperCase(java.util.Locale.ROOT)) + 16;
    }

    // ------------------------------------------------------------------
    // Sidebar icons, drawn in a 12x12 box from its top-left corner.
    // ------------------------------------------------------------------

    public static void iconModules(GuiGraphics g, int x, int y, int c) {
        g.fill(x, y, x + 5, y + 5, c);
        g.fill(x + 7, y, x + 12, y + 5, c);
        g.fill(x, y + 7, x + 5, y + 12, c);
        g.fill(x + 7, y + 7, x + 12, y + 12, c);
    }

    public static void iconCosmetics(GuiGraphics g, int x, int y, int c) {
        // A shirt: shoulders, sleeves and body.
        g.fill(x + 3, y + 1, x + 9, y + 3, c);
        g.fill(x, y + 2, x + 3, y + 6, c);
        g.fill(x + 9, y + 2, x + 12, y + 6, c);
        g.fill(x + 3, y + 3, x + 9, y + 12, c);
    }

    public static void iconWaypoints(GuiGraphics g, int x, int y, int c) {
        // A map pin: a round head tapering to a point.
        for (int i = 0; i < 7; i++) {
            int half = i < 3 ? 2 + i : 5 - (i - 3);
            g.fill(x + 6 - half, y + i, x + 6 + half, y + i + 1, c);
        }
        g.fill(x + 4, y + 7, x + 8, y + 9, c);
        g.fill(x + 5, y + 9, x + 7, y + 12, c);
    }

    /** A paint palette; {@code bg} punches the hole so it reads as a ring. */
    public static void iconThemes(GuiGraphics g, int x, int y, int c, int bg) {
        round(g, x, y, 12, 12, 6, c);
        round(g, x + 2, y + 2, 8, 8, 4, bg);
        g.fill(x + 3, y + 4, x + 5, y + 6, c);
        g.fill(x + 6, y + 3, x + 8, y + 5, c);
        g.fill(x + 5, y + 7, x + 7, y + 9, c);
    }

    public static void iconSettings(GuiGraphics g, int x, int y, int c) {
        // Three slider tracks with their knobs at different heights.
        for (int i = 0; i < 3; i++) {
            int tx = x + 1 + i * 4;
            g.fill(tx + 1, y, tx + 2, y + 12, c);
            int ky = y + new int[] {7, 2, 5}[i];
            g.fill(tx, ky, tx + 3, ky + 3, c);
        }
    }

    /** The grid-view icon at 10x8, for the view switch. */
    public static void iconModulesSmall(GuiGraphics g, int x, int y, int c) {
        g.fill(x, y, x + 4, y + 3, c);
        g.fill(x + 6, y, x + 10, y + 3, c);
        g.fill(x, y + 5, x + 4, y + 8, c);
        g.fill(x + 6, y + 5, x + 10, y + 8, c);
    }

    /** The "adjust" icon at 9x9, beside OPTIONS on a card. */
    public static void iconSettingsSmall(GuiGraphics g, int x, int y, int c) {
        for (int i = 0; i < 3; i++) {
            int tx = x + i * 3;
            g.fill(tx + 1, y, tx + 2, y + 9, c);
            int ky = y + new int[] {5, 1, 3}[i];
            g.fill(tx, ky, tx + 3, ky + 2, c);
        }
    }

    /** The logo mark: a diamond with a smaller diamond inside, as in the design. */
    public static void logo(GuiGraphics g, int cx, int cy, int c) {
        for (int i = -6; i <= 6; i++) {
            int half = 6 - Math.abs(i);
            g.fill(cx - half, cy + i, cx + half + 1, cy + i + 1, c);
        }
        for (int i = -3; i <= 3; i++) {
            int half = 3 - Math.abs(i);
            g.fill(cx - half, cy + i, cx + half + 1, cy + i + 1, 0x99000000);
        }
    }
}
