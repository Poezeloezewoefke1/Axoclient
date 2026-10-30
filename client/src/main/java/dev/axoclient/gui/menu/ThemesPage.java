package dev.axoclient.gui.menu;

import dev.axoclient.gui.theme.Palette;
import dev.axoclient.gui.theme.Themes;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The Themes page: one card per {@link Palette}, each a miniature of the menu
 * in that theme's colours. Picking one recolours the whole menu at once, and
 * every other Axo screen with it.
 */
final class ThemesPage implements MenuPage {
    private static final int CARD_MIN_W = 112;
    private static final int CARD_H = 70;
    private static final int GAP = 8;

    private int scroll;
    private int maxScroll;

    @Override
    public String navLabel() {
        return "Themes";
    }

    @Override
    public String navCount() {
        return Integer.toString(Palette.ALL.size());
    }

    @Override
    public String titleAccent() {
        return "Client";
    }

    @Override
    public String titleRest() {
        return "Themes";
    }

    @Override
    public String subtitle() {
        return Palette.ALL.size() + " themes · Active: " + Themes.palette().name();
    }

    @Override
    public void render(GuiGraphics g, Palette p, Hits hits, int x, int y, int w, int h, int mx, int my, String query) {
        List<Palette> all = Palette.ALL;
        int cols = Math.max(2, Math.min(5, (w + GAP) / (CARD_MIN_W + GAP)));
        int cardW = (w - GAP * (cols - 1)) / cols;
        int rows = (all.size() + cols - 1) / cols;
        maxScroll = Math.max(0, rows * (CARD_H + GAP) - GAP - h);
        scroll = Math.min(scroll, maxScroll);

        g.enableScissor(x, y, x + w, y + h);
        hits.clip(x, y, w, h);
        for (int i = 0; i < all.size(); i++) {
            Palette t = all.get(i);
            int cx = x + (i % cols) * (cardW + GAP);
            int cy = y - scroll + (i / cols) * (CARD_H + GAP);
            boolean active = t.id().equals(p.id());
            boolean hover = Hits.over(mx, my, cx, cy, cardW, CARD_H);
            Draw.card(g, cx, cy, cardW, CARD_H, 8, p.card(), active ? p.accent() : (hover ? p.line() : p.border()));
            if (active) {
                Draw.card(g, cx + 1, cy + 1, cardW - 2, CARD_H - 2, 7, p.card(), p.accent());
            }
            miniature(g, t, cx + 2, cy + 2, cardW - 4, 46);

            Draw.text(g, Draw.trim(t.name(), cardW - 60), cx + 8, cy + 54, p.text());
            Draw.round(g, cx + cardW - 14, cy + 54, 7, 7, 3, t.accent());
            if (active) {
                Draw.label(g, "Active", cx + cardW - 18 - Draw.width("ACTIVE"), cy + 54, p.accent());
            }
            hits.add(cx, cy, cardW, CARD_H, () -> Themes.setPalette(t));
        }
        hits.unclip();
        g.disableScissor();
    }

    /** The menu in miniature: a sidebar with three bars, and a panel of four cards. */
    private static void miniature(GuiGraphics g, Palette t, int x, int y, int w, int h) {
        Draw.round(g, x, y, w, h, 6, t.ground());
        g.fill(x, y + h - 6, x + w, y + h, t.ground());
        int side = 22;
        Draw.round(g, x + 6, y + 6, side, h - 12, 3, t.side());
        g.fill(x + 9, y + 10, x + side + 3, y + 13, t.accent());
        g.fill(x + 9, y + 16, x + side + 3, y + 19, t.line());
        g.fill(x + 9, y + 22, x + side + 3, y + 25, t.line());

        int px = x + side + 10;
        int pw = w - side - 16;
        Draw.round(g, px, y + 6, pw, h - 12, 3, t.panel());
        int cw = (pw - 9) / 2;
        int ch = (h - 12 - 9) / 2;
        for (int i = 0; i < 4; i++) {
            int cx = px + 3 + (i % 2) * (cw + 3);
            int cy = y + 9 + (i / 2) * (ch + 3);
            Draw.round(g, cx, cy, cw, ch, 2, t.card());
            if (i != 2) {
                g.fill(cx, cy + ch - 2, cx + cw, cy + ch, t.accent());
            }
        }
    }

    @Override
    public boolean scroll(double mx, double my, double amount) {
        scroll = Math.max(0, Math.min(maxScroll, scroll + (int) Math.round(-amount * 24)));
        return true;
    }
}
