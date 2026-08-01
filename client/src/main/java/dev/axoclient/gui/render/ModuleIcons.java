package dev.axoclient.gui.render;

import dev.axoclient.core.ModuleCategory;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Pictograms drawn for each module card, one per category.
 *
 * Built from filled rectangles rather than texture assets: the card grid
 * needs an icon for every module, and hand-drawing fifty of them — or
 * shipping fifty PNGs — buys very little over a clear per-category symbol.
 * Diagonals are stepped a pixel at a time, since fill() only does axis-aligned
 * boxes.
 *
 * Every icon draws inside the box it is given and is centred on it, so the
 * caller only has to decide where the icon area is.
 */
public final class ModuleIcons {

    private ModuleIcons() {
    }

    /** Draw {@code category}'s pictogram centred in the given box. */
    public static void draw(GuiGraphics g, ModuleCategory category, int x, int y, int w, int h, int colour) {
        int cx = x + w / 2;
        int cy = y + h / 2;
        switch (category) {
            case HUD -> hud(g, cx, cy, colour);
            case PVP -> pvp(g, cx, cy, colour);
            case COSMETIC -> cosmetic(g, cx, cy, colour);
            case PERFORMANCE -> performance(g, cx, cy, colour);
            case QOL -> qol(g, cx, cy, colour);
        }
    }

    /** Three stacked bars, like a readout. */
    private static void hud(GuiGraphics g, int cx, int cy, int colour) {
        int[] widths = {20, 14, 17};
        for (int i = 0; i < widths.length; i++) {
            GuiRender.rect(g, cx - 10, cy - 8 + i * 7, widths[i], 4, colour);
        }
    }

    /** A shield: square shoulders tapering to a point. */
    private static void pvp(GuiGraphics g, int cx, int cy, int colour) {
        GuiRender.rect(g, cx - 9, cy - 10, 18, 9, colour);
        for (int i = 0; i < 9; i++) {
            int inset = i;
            GuiRender.rect(g, cx - 9 + inset, cy - 1 + i, (9 - inset) * 2, 1, colour);
        }
    }

    /** A four-point star, drawn as two tapering bars. */
    private static void cosmetic(GuiGraphics g, int cx, int cy, int colour) {
        for (int i = 0; i < 10; i++) {
            int half = Math.max(1, 5 - Math.abs(i - 5) / 2);
            GuiRender.rect(g, cx - half, cy - 10 + i * 2, half * 2, 2, colour);
        }
        for (int i = 0; i < 10; i++) {
            int half = Math.max(1, 5 - Math.abs(i - 5) / 2);
            GuiRender.rect(g, cx - 10 + i * 2, cy - half, 2, half * 2, colour);
        }
    }

    /** A rising bar chart. */
    private static void performance(GuiGraphics g, int cx, int cy, int colour) {
        int[] heights = {5, 10, 15, 20};
        for (int i = 0; i < heights.length; i++) {
            GuiRender.rect(g, cx - 11 + i * 6, cy + 10 - heights[i], 4, heights[i], colour);
        }
    }

    /** A gear: a ring with four teeth. */
    private static void qol(GuiGraphics g, int cx, int cy, int colour) {
        ring(g, cx, cy, 8, 3, colour);
        GuiRender.rect(g, cx - 2, cy - 12, 4, 4, colour);
        GuiRender.rect(g, cx - 2, cy + 8, 4, 4, colour);
        GuiRender.rect(g, cx - 12, cy - 2, 4, 4, colour);
        GuiRender.rect(g, cx + 8, cy - 2, 4, 4, colour);
    }

    /**
     * An approximate circular ring, one row of pixels at a time.
     *
     * For each row the horizontal half-width of the outer circle is computed
     * from the circle equation, and the inner circle is subtracted, leaving
     * two spans. Rows where the inner circle does not reach draw as one span.
     */
    private static void ring(GuiGraphics g, int cx, int cy, int radius, int thickness, int colour) {
        int inner = Math.max(1, radius - thickness);
        for (int dy = -radius; dy <= radius; dy++) {
            int outerHalf = (int) Math.round(Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            if (outerHalf == 0) {
                continue;
            }
            int innerHalf = Math.abs(dy) < inner
                ? (int) Math.round(Math.sqrt(Math.max(0, inner * inner - dy * dy)))
                : 0;
            if (innerHalf == 0) {
                GuiRender.rect(g, cx - outerHalf, cy + dy, outerHalf * 2, 1, colour);
            } else {
                GuiRender.rect(g, cx - outerHalf, cy + dy, outerHalf - innerHalf, 1, colour);
                GuiRender.rect(g, cx + innerHalf, cy + dy, outerHalf - innerHalf, 1, colour);
            }
        }
    }

    /**
     * A small gear for the per-card settings button, sized to fit inside it.
     * The category pictogram version is drawn at a fixed 24px and would spill
     * out of a 20px-tall button.
     */
    public static void gear(GuiGraphics g, int cx, int cy, int radius, int colour) {
        ring(g, cx, cy, radius, Math.max(1, radius / 2), colour);
        int tooth = Math.max(1, radius / 2);
        GuiRender.rect(g, cx - tooth / 2, cy - radius - tooth, tooth, tooth, colour);
        GuiRender.rect(g, cx - tooth / 2, cy + radius, tooth, tooth, colour);
        GuiRender.rect(g, cx - radius - tooth, cy - tooth / 2, tooth, tooth, colour);
        GuiRender.rect(g, cx + radius, cy - tooth / 2, tooth, tooth, colour);
    }

    /** A five-point star used for the favourite toggle, filled or outlined. */
    public static void star(GuiGraphics g, int cx, int cy, int size, boolean filled, int colour) {
        int half = size / 2;
        if (filled) {
            for (int i = 0; i < size; i++) {
                int w = Math.max(1, half - Math.abs(i - half) + 2);
                GuiRender.rect(g, cx - w, cy - half + i, w * 2, 1, colour);
            }
            return;
        }
        GuiRender.rect(g, cx - half, cy - half, size, 1, colour);
        GuiRender.rect(g, cx - half, cy + half, size, 1, colour);
        GuiRender.rect(g, cx - half, cy - half, 1, size, colour);
        GuiRender.rect(g, cx + half, cy - half, 1, size + 1, colour);
    }
}
