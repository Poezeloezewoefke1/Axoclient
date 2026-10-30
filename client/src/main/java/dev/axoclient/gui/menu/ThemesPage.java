package dev.axoclient.gui.menu;

import dev.axoclient.gui.menu.Ui.Type;
import dev.axoclient.gui.theme.Palette;
import dev.axoclient.gui.theme.Themes;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The Themes page: one card per {@link Palette}, a miniature of the menu in
 * that theme's colours (a sidebar with three bars, a panel of four cards).
 * Picking one recolours the whole menu and every other Axo screen.
 */
final class ThemesPage implements MenuPage {
    private static final int GAP = 12;
    private static final int CARD_H = 2 + 92 + 36 + 2;

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
        int innerW = w - 4;
        int cardW = (innerW - GAP * 3) / 4;
        int rows = (all.size() + 3) / 4;
        int contentH = rows * (CARD_H + GAP) - GAP + 24;
        maxScroll = Math.max(0, contentH - h);
        scroll = Math.min(scroll, maxScroll);

        g.enableScissor(x, y, x + w, y + h);
        hits.clip(x, y, w, h);
        for (int i = 0; i < all.size(); i++) {
            Palette t = all.get(i);
            int cx = x + (i % 4) * (cardW + GAP);
            int cy = y - scroll + (i / 4) * (CARD_H + GAP);
            boolean active = t.id().equals(p.id());
            if (Hits.over(mx, my, cx, cy, cardW, CARD_H)) {
                cy -= 2;
            }
            Ui.box(g, cx, cy, cardW, CARD_H, 14, p.card(), active ? p.accent() : p.border(), 2);

            // Preview: 92px, padding 12, gap 8, on the theme's ground; top corners follow the card.
            int px = cx + 2;
            int py = cy + 2;
            int pw = cardW - 4;
            Ui.round(g, px, py, pw, 92, 12, t.ground());
            Ui.rect(g, px, py + 80, pw, 12, t.ground());
            Ui.round(g, px + 12, py + 12, 40, 68, 6, t.side());
            Ui.round(g, px + 18, py + 18, 28, 5, 3, t.accent());
            Ui.round(g, px + 18, py + 28, 28, 5, 3, t.line());
            Ui.round(g, px + 18, py + 38, 28, 5, 3, t.line());
            int panX = px + 12 + 40 + 8;
            int panW = pw - 12 - 40 - 8 - 12;
            Ui.round(g, panX, py + 12, panW, 68, 6, t.panel());
            int cw = (panW - 12 - 5) / 2;
            int ch = (68 - 12 - 5) / 2;
            for (int k = 0; k < 4; k++) {
                int bx = panX + 6 + (k % 2) * (cw + 5);
                int by = py + 12 + 6 + (k / 2) * (ch + 5);
                Ui.round(g, bx, by, cw, ch, 4, t.card());
                if (k != 2) {
                    Ui.round(g, bx, by + ch - 4, cw, 4, 2, t.accent());
                    Ui.rect(g, bx, by + ch - 4, cw, 2, t.accent());
                }
            }

            // Footer: padding 10 12 — name, ACTIVE, accent dot.
            int fy = cy + 2 + 92 + 18;
            Ui.round(g, cx + cardW - 12 - 12, fy - 6, 12, 12, 6, t.accent());
            int nameRight = cx + cardW - 12 - 12 - 8;
            if (active) {
                int aw = Ui.spacedWidth(Type.B9, "ACTIVE", 0.14f);
                Ui.spaced(g, Type.B9, "ACTIVE", nameRight - aw, fy, p.accent(), 0.14f);
                nameRight -= aw + 8;
            }
            Ui.mid(g, Type.M13, Ui.trim(Type.M13, t.name(), nameRight - cx - 14), cx + 14, fy, p.text());
            hits.add(cx, cy, cardW, CARD_H, () -> Themes.setPalette(t));
        }
        hits.unclip();
        g.disableScissor();
        ModulesPage.scrollbar(g, x + w, y, h, contentH, scroll);
    }

    @Override
    public boolean scroll(double mx, double my, double amount) {
        scroll = Math.max(0, Math.min(maxScroll, scroll + (int) Math.round(-amount * 40)));
        return true;
    }
}
