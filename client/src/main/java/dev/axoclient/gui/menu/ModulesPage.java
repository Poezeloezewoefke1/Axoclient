package dev.axoclient.gui.menu;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleDescriptions;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.core.ModuleSetting;
import dev.axoclient.gui.menu.Ui.Type;
import dev.axoclient.gui.theme.Palette;
import dev.axoclient.hud.HudModule;
import dev.axoclient.input.Keybinds;
import dev.axoclient.modules.hud.CrosshairModule;
import dev.axoclient.modules.hud.HudStyleModule;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/**
 * The Modules page: filter chips, the card grid (or list), and each module's
 * options page with a live preview. Sizes are the design's: 4-column grid of
 * 198px cards with 14px gaps, 112px card tops, 34px OPTIONS row and state bar.
 *
 * Options rows are generated from each module's own settings, so every module
 * gets one: On/Off choices become switches, short choices the mode buttons,
 * long choices arrows, numbers sliders, keys a press-a-key button.
 */
final class ModulesPage implements MenuPage {
    private static final String[] CHIPS = {"ALL", "NEW", "HUD", "PVP", "QOL", "PERFORMANCE"};
    private static final int GRID_GAP = 14;
    private static final int CARD_H = 183;
    private static final int ROW_H = 62;
    private static final int PREVIEW_W = 250;

    private int chip;
    private int scroll;
    private int maxScroll;

    private AxoModule optionsFor;
    private int optionsScroll;
    private int optionsMaxScroll;
    private boolean listeningToggleKey;
    private ModuleSetting listeningSetting;
    private ModuleSetting draggingSlider;
    private int sliderX;
    private int sliderW;

    @Override
    public String navLabel() {
        return "Modules";
    }

    @Override
    public String navCount() {
        return Integer.toString(ModuleManager.get().all().size());
    }

    @Override
    public String titleAccent() {
        return optionsFor != null ? "Module" : "Client";
    }

    @Override
    public String titleRest() {
        return optionsFor != null ? "Options" : "Modules";
    }

    @Override
    public String subtitle() {
        if (optionsFor != null) {
            int n = optionsFor.settings().size();
            return "Modules › " + optionsFor.displayName() + " · " + n + (n == 1 ? " setting" : " settings");
        }
        long enabled = ModuleManager.get().all().stream().filter(ModuleManager.get()::isEnabled).count();
        return ModuleManager.get().all().size() + " modules · " + enabled + " enabled";
    }

    @Override
    public String searchPlaceholder() {
        return optionsFor == null ? "Search modules" : null;
    }

    @Override
    public void shown() {
        optionsFor = null;
        listeningToggleKey = false;
        listeningSetting = null;
        draggingSlider = null;
    }

    void openOptions(AxoModule module) {
        optionsFor = module;
        optionsScroll = 0;
        listeningToggleKey = false;
        listeningSetting = null;
    }

    private List<AxoModule> visible(String query) {
        String needle = query.toLowerCase(Locale.ROOT).trim();
        List<AxoModule> out = new ArrayList<>();
        for (AxoModule module : ModuleManager.get().all()) {
            boolean keep = switch (chip) {
                case 1 -> ModuleDescriptions.isNew(module.id());
                case 2 -> module.category() == ModuleCategory.HUD;
                case 3 -> module.category() == ModuleCategory.PVP;
                case 4 -> module.category() == ModuleCategory.QOL;
                case 5 -> module.category() == ModuleCategory.PERFORMANCE;
                default -> true;
            };
            if (keep && !needle.isEmpty()) {
                keep = module.displayName().toLowerCase(Locale.ROOT).contains(needle)
                    || module.description().toLowerCase(Locale.ROOT).contains(needle);
            }
            if (keep) {
                out.add(module);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Grid / list
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, Palette p, Hits hits, int x, int y, int w, int h, int mx, int my, String query) {
        if (optionsFor != null) {
            renderOptions(g, p, hits, x, y, w, h, mx, my);
            return;
        }
        // Chips: height 30, padding 0 14, gap 8.
        int cx = x;
        for (int i = 0; i < CHIPS.length; i++) {
            int cw = Ui.chip(g, p, CHIPS[i], cx, y, chip == i, 14);
            int index = i;
            hits.add(cx, y, cw, 30, () -> {
                chip = index;
                scroll = 0;
            });
            cx += cw + 8;
        }
        // List / grid switch: raised strip, padding 3, two 32x28 buttons 4 apart.
        boolean list = MenuSettings.listView();
        int sx = x + w - 74;
        int sy = y - 2;
        Ui.round(g, sx, sy, 74, 34, 9, p.raised());
        Ui.round(g, sx + 3, sy + 3, 32, 28, 7, list ? p.accentSoft() : 0);
        Ui.round(g, sx + 39, sy + 3, 32, 28, 7, list ? 0 : p.accentSoft());
        Ui.icon(g, MenuIcon.LIST, sx + 3 + 8, sy + 3 + 6, 15, list ? p.accent() : p.sub());
        Ui.icon(g, MenuIcon.GRID, sx + 39 + 8, sy + 3 + 6, 15, list ? p.sub() : p.accent());
        hits.add(sx + 3, sy + 3, 32, 28, () -> MenuSettings.setListView(true));
        hits.add(sx + 39, sy + 3, 32, 28, () -> MenuSettings.setListView(false));

        // Scroll area: 16px below the chips, padding 0 4 24 0.
        int top = y + 30 + 16;
        int viewH = y + h - top;
        int innerW = w - 4;
        List<AxoModule> modules = visible(query);
        if (modules.isEmpty()) {
            Ui.midCentered(g, Type.R13, "No modules match your search.", x + innerW / 2, top + 75, p.sub());
            return;
        }
        g.enableScissor(x, top, x + w, top + viewH);
        hits.clip(x, top, w, viewH);
        int contentH = list ? renderList(g, p, hits, modules, x, top - scroll, innerW)
                            : renderGrid(g, p, hits, modules, x, top - scroll, innerW, mx, my);
        hits.unclip();
        g.disableScissor();
        maxScroll = Math.max(0, contentH + 24 - viewH);
        scroll = Math.min(scroll, maxScroll);
        scrollbar(g, x + w - 2, top, viewH, contentH + 24, scroll);
    }

    /** The design's thin scrollbar: 6px wide, rgba(128,128,128,.35), radius 3. */
    static void scrollbar(GuiGraphics g, int x, int y, int viewH, int contentH, int scroll) {
        if (contentH <= viewH) {
            return;
        }
        int thumbH = Math.max(30, viewH * viewH / contentH);
        int thumbY = y + (int) ((viewH - thumbH) * (scroll / (double) (contentH - viewH)));
        Ui.round(g, x - 4, thumbY, 6, thumbH, 3, 0x59808080);
    }

    private int renderGrid(GuiGraphics g, Palette p, Hits hits, List<AxoModule> modules, int x, int y, int w,
                           int mx, int my) {
        int cardW = (w - GRID_GAP * 3) / 4;
        for (int i = 0; i < modules.size(); i++) {
            int cx = x + (i % 4) * (cardW + GRID_GAP);
            int cy = y + (i / 4) * (CARD_H + GRID_GAP);
            card(g, p, hits, modules.get(i), cx, cy, cardW, mx, my);
        }
        int rows = (modules.size() + 3) / 4;
        return rows * (CARD_H + GRID_GAP) - GRID_GAP;
    }

    private void card(GuiGraphics g, Palette p, Hits hits, AxoModule module, int x, int y, int w, int mx, int my) {
        boolean on = ModuleManager.get().isEnabled(module);
        // .axo-card:hover lifts the card by 2px.
        int lift = Hits.over(mx, my, x, y, w, CARD_H) ? -2 : 0;
        y += lift;
        Ui.box(g, x, y, w, CARD_H, 12, p.card(), p.border());

        if (ModuleDescriptions.isNew(module.id())) {
            int bw = Ui.spacedWidth(Type.B9, "NEW", 0.12f) + 14;
            Ui.round(g, x + 10, y + 10, bw, 17, 5, p.accentSoft());
            Ui.spacedCentered(g, Type.B9, "NEW", x + 10 + bw / 2, y + 18, p.accent(), 0.12f);
        }
        // Icon (34px) and name, centred in the 112px top.
        Ui.icon(g, MenuIcon.of(module), x + w / 2 - 17, y + 1 + 24, 34, on ? p.text() : p.muted());
        String name = Ui.trim(Type.M14, module.displayName(), w - 20);
        Ui.midCentered(g, Type.M14, name, x + w / 2, y + 1 + 24 + 34 + 12 + 8, on ? p.text() : p.sub());
        int key = Keybinds.keyOf(module);
        if (MenuSettings.keybindsOnCards() && key > 0) {
            Ui.midCentered(g, Type.R10, Keybinds.keyName(key).toUpperCase(Locale.ROOT), x + w / 2, y + 100, p.muted());
        }

        // OPTIONS | sliders row: border-top, 34px, raised.
        int rowY = y + 1 + 112;
        Ui.rect(g, x + 1, rowY, w - 2, 1, p.border());
        Ui.rect(g, x + 1, rowY + 1, w - 2, 34, p.raised());
        Ui.rect(g, x + w - 1 - 40, rowY + 1, 1, 34, p.border());
        Ui.spacedCentered(g, Type.S10, "OPTIONS", x + 1 + (w - 2 - 40) / 2, rowY + 18, p.sub(), 0.18f);
        Ui.icon(g, MenuIcon.SLIDERS, x + w - 1 - 20 - 7, rowY + 1 + 10, 14, p.sub());
        hits.add(x, rowY, w, 35, () -> openOptions(module));

        // ENABLED / DISABLED bar, rounded with the card's bottom corners.
        int barY = rowY + 35;
        int barColour = on ? p.accent() : p.raised();
        Ui.round(g, x + 1, barY, w - 2, 34, 11, barColour);
        Ui.rect(g, x + 1, barY, w - 2, 12, barColour);
        Ui.spacedCentered(g, Type.B10, on ? "ENABLED" : "DISABLED", x + w / 2, barY + 17, on ? p.onAccent() : p.sub(), 0.2f);
        hits.add(x, barY, w, 34, () -> ModuleManager.get().toggle(module));
    }

    private int renderList(GuiGraphics g, Palette p, Hits hits, List<AxoModule> modules, int x, int y, int w) {
        for (int i = 0; i < modules.size(); i++) {
            AxoModule module = modules.get(i);
            int ry = y + i * (ROW_H + 8);
            boolean on = ModuleManager.get().isEnabled(module);
            Ui.box(g, x, ry, w, ROW_H, 12, p.card(), p.border());
            int mid = ry + ROW_H / 2;
            Ui.round(g, x + 17, mid - 18, 36, 36, 9, p.raised());
            Ui.icon(g, MenuIcon.of(module), x + 17 + 9, mid - 9, 18, on ? p.text() : p.muted());

            int right = x + w - 17;
            // Toggle 38x22, options chevron 32x32, category label.
            Ui.toggle(g, right - 38, mid - 11, 38, 22, on, p);
            hits.add(right - 40, ry, 42, ROW_H, () -> ModuleManager.get().toggle(module));
            int chevX = right - 38 - 14 - 32;
            Ui.icon(g, MenuIcon.CHEVRON_RIGHT, chevX + 8, mid - 8, 15, p.sub());
            String cat = module.category().displayName().toUpperCase(Locale.ROOT);
            int catW = Ui.spacedWidth(Type.S10, cat, 0.12f);
            Ui.spaced(g, Type.S10, cat, chevX - 14 - catW, mid, p.muted(), 0.12f);

            int textX = x + 17 + 36 + 14;
            int textW = chevX - 14 - catW - 14 - textX;
            Ui.text(g, Type.S14, Ui.trim(Type.S14, module.displayName(), textW), textX, mid - 16, p.text());
            Ui.text(g, Type.R11, Ui.trim(Type.R11, module.description(), textW), textX, mid + 3, p.muted());
            hits.add(x, ry, right - 40 - x, ROW_H, () -> openOptions(module));
        }
        return modules.size() * (ROW_H + 8) - 8;
    }

    // ------------------------------------------------------------------
    // Options
    // ------------------------------------------------------------------

    private void renderOptions(GuiGraphics g, Palette p, Hits hits, int x, int y, int w, int h, int mx, int my) {
        AxoModule module = optionsFor;
        int bottom = y + h - 22;
        int leftW = w - PREVIEW_W - 16;
        boolean on = ModuleManager.get().isEnabled(module);

        // Header card: padding 14 16 → 73px tall.
        int hh = 73;
        int mid = y + hh / 2;
        Ui.box(g, x, y, leftW, hh, 12, p.card(), p.border());
        Ui.box(g, x + 17, mid - 18, 36, 36, 9, p.raised(), p.border());
        Ui.icon(g, MenuIcon.CHEVRON_LEFT, x + 17 + 10, mid - 8, 16, p.text());
        hits.add(x + 17, mid - 18, 36, 36, () -> optionsFor = null);
        Ui.round(g, x + 17 + 36 + 14, mid - 21, 42, 42, 10, p.accentSoft());
        Ui.icon(g, MenuIcon.of(module), x + 17 + 36 + 14 + 10, mid - 11, 22, p.accent());

        int right = x + leftW - 17;
        // State column: label above a 44x26 switch.
        String state = on ? "ENABLED" : "DISABLED";
        Ui.spaced(g, Type.S9, state, right - Ui.spacedWidth(Type.S9, state, 0.12f), mid - 15, p.muted(), 0.12f);
        Ui.toggle(g, right - 44, mid - 4, 44, 26, on, p);
        hits.add(right - 46, mid - 6, 48, 30, () -> ModuleManager.get().toggle(module));
        // Keybind column, 14px to the left.
        int stateW = Math.max(44, Ui.spacedWidth(Type.S9, state, 0.12f));
        int keyRight = right - stateW - 14;
        String keyLabel = listeningToggleKey ? "PRESS A KEY" : keyText(Keybinds.keyOf(module));
        int keyW = Math.max(64, Ui.spacedWidth(Type.B11, keyLabel, 0.1f) + 20);
        Ui.spaced(g, Type.S9, "KEYBIND", keyRight - Ui.spacedWidth(Type.S9, "KEYBIND", 0.12f), mid - 15, p.muted(), 0.12f);
        Ui.box(g, keyRight - keyW, mid - 5, keyW, 28, 7, p.raised(), listeningToggleKey ? p.accent() : p.border());
        Ui.spacedCentered(g, Type.B11, keyLabel, keyRight - keyW / 2, mid + 9, listeningToggleKey ? p.accent() : p.text(), 0.1f);
        hits.add(keyRight - keyW, mid - 5, keyW, 28, () -> {
            listeningToggleKey = true;
            listeningSetting = null;
        });

        int textX = x + 17 + 36 + 14 + 42 + 14;
        int textW = keyRight - keyW - 14 - textX;
        Ui.text(g, Type.S16, Ui.trim(Type.S16, module.displayName(), textW), textX, mid - 18, p.text());
        Ui.text(g, Type.R11, Ui.trim(Type.R11, module.description(), textW), textX, mid + 4, p.sub());

        // Rows: 12px below the header, 62px tall, 8px apart; then Reset to defaults.
        int top = y + hh + 12;
        int viewH = bottom - top;
        List<ModuleSetting> settings = module.settings();
        g.enableScissor(x, top, x + leftW, bottom);
        hits.clip(x, top, leftW, viewH);
        int ry = top - optionsScroll;
        int rowW = leftW - 4;
        if (settings.isEmpty()) {
            Ui.box(g, x, ry, rowW, ROW_H, 12, p.card(), p.border());
            Ui.mid(g, Type.S13, "This module has nothing to adjust.", x + 17, ry + ROW_H / 2, p.sub());
            ry += ROW_H + 8;
        }
        for (ModuleSetting setting : settings) {
            Ui.box(g, x, ry, rowW, ROW_H, 12, p.card(), p.border());
            int controlW = renderControl(g, p, hits, setting, x + rowW - 17, ry + ROW_H / 2, mx);
            String label = Ui.trim(Type.S13, setting.label(), rowW - 34 - controlW - 14);
            Ui.mid(g, Type.S13, label, x + 17, ry + ROW_H / 2, p.text());
            ry += ROW_H + 8;
        }
        if (!settings.isEmpty()) {
            String reset = "Reset to defaults";
            int rw = Ui.width(Type.S11, reset) + 28;
            int resetY = ry + 4 - 8;
            // Outline-only button: "fill" with the panel it sits on.
            Ui.box(g, x + rowW - rw, resetY, rw, 32, 8, p.panel(), p.border());
            Ui.midCentered(g, Type.S11, reset, x + rowW - rw / 2, resetY + 16, p.sub());
            hits.add(x + rowW - rw, resetY, rw, 32, () -> settings.forEach(ModuleSetting::reset));
            ry = resetY + 32 + 2;
        }
        hits.unclip();
        g.disableScissor();
        int contentH = ry + optionsScroll - top;
        optionsMaxScroll = Math.max(0, contentH - viewH);
        optionsScroll = Math.min(optionsScroll, optionsMaxScroll);
        scrollbar(g, x + leftW, top, viewH, contentH, optionsScroll);

        renderPreview(g, p, module, x + leftW + 16, y, PREVIEW_W, bottom - y);
    }

    private static String keyText(int key) {
        return key > 0 ? Keybinds.keyName(key).toUpperCase(Locale.ROOT) : "NONE";
    }

    /** One setting's control, right-aligned at {@code right} and centred on {@code mid}. Returns its width. */
    private int renderControl(GuiGraphics g, Palette p, Hits hits, ModuleSetting setting, int right, int mid, int mx) {
        if (setting.isSwitch()) {
            Ui.toggle(g, right - 38, mid - 11, 38, 22, setting.switchOn(), p);
            hits.add(right - 42, mid - 15, 46, 30, () -> setting.setSwitch(!setting.switchOn()));
            return 38;
        }
        switch (setting.format()) {
            case CHOICE -> {
                List<String> choices = setting.choices();
                int segW = Ui.segmentedWidth(Type.S11, choices, 11);
                if (segW <= 320) {
                    int[] edges = Ui.segmented(g, p, right, mid - 17, 34, choices, setting.get(), Type.S11, 11, 7);
                    for (int i = 0; i < choices.size(); i++) {
                        int index = i;
                        hits.add(edges[i], mid - 17, edges[i + 1] - edges[i], 34, () -> setting.set(index));
                    }
                    return segW;
                }
                // Too many options for one row: arrows either side of the current one.
                int cw = 190;
                int cx = right - cw;
                Ui.round(g, cx, mid - 17, cw, 34, 9, p.raised());
                Ui.icon(g, MenuIcon.CHEVRON_LEFT, cx + 8, mid - 7, 14, p.sub());
                Ui.icon(g, MenuIcon.CHEVRON_RIGHT, cx + cw - 22, mid - 7, 14, p.sub());
                Ui.midCentered(g, Type.S11, Ui.trim(Type.S11, setting.display(), cw - 56), cx + cw / 2, mid, p.text());
                hits.add(cx, mid - 17, cw / 2, 34, () -> setting.nudge(-1));
                hits.add(cx + cw / 2, mid - 17, cw - cw / 2, 34, () -> setting.nudge(1));
                return cw;
            }
            case KEY -> {
                boolean listening = listeningSetting == setting;
                String label = listening ? "PRESS A KEY" : keyText(setting.get());
                int kw = Math.max(64, Ui.spacedWidth(Type.B11, label, 0.1f) + 20);
                Ui.box(g, right - kw, mid - 14, kw, 28, 7, p.raised(), listening ? p.accent() : p.border());
                Ui.spacedCentered(g, Type.B11, label, right - kw / 2, mid, listening ? p.accent() : p.text(), 0.1f);
                hits.add(right - kw, mid - 14, kw, 28, () -> {
                    listeningSetting = setting;
                    listeningToggleKey = false;
                });
                return kw;
            }
            default -> {
                // input[type=range] 160px + a 52x26 value box, 10px apart.
                int boxW = 52;
                int trackW = 160;
                int tx = right - boxW - 10 - trackW;
                double f = setting.fraction();
                int filled = (int) Math.round(f * trackW);
                Ui.round(g, tx, mid - 2, trackW, 4, 2, p.off());
                Ui.round(g, tx, mid - 2, Math.max(4, filled), 4, 2, p.accent());
                Ui.round(g, tx + filled - 8, mid - 8, 16, 16, 8, p.accent());
                Ui.round(g, right - boxW, mid - 13, boxW, 26, 6, p.raised());
                Ui.midCentered(g, Type.S11, Ui.trim(Type.S11, setting.display(), boxW - 6), right - boxW / 2, mid, p.text());
                hits.add(tx - 8, mid - 15, trackW + 16, 30, () -> {
                    draggingSlider = setting;
                    sliderX = tx;
                    sliderW = trackW;
                    setting.setFraction((mx - tx) / (double) trackW);
                });
                return boxW + 10 + trackW;
            }
        }
    }

    /** LIVE PREVIEW: a sky/grass/dirt scene with the module's real output on it. */
    private static void renderPreview(GuiGraphics g, Palette p, AxoModule module, int x, int y, int w, int h) {
        Ui.box(g, x, y, w, h, 14, p.card(), p.border());
        Ui.spaced(g, Type.S10, "LIVE PREVIEW", x + 17, y + 17 + 6, p.muted(), 0.12f);

        List<String> note = Ui.wrap(Type.R11, module.description(), w - 34);
        int noteH = Math.round(note.size() * 16.5f);
        int sx = x + 17;
        int sy = y + 17 + 12 + 12;
        int sw = w - 34;
        int sh = (y + h - 17) - noteH - 12 - sy;
        // background: #5E7F9E, linear-gradient(transparent 62%, #4E7A3A 62% 66%, #6B5236 66%)
        Ui.round(g, sx, sy, sw, sh, 10, 0xFF6B5236);
        Ui.round(g, sx, sy, sw, Math.round(sh * 0.62f), 10, 0xFF5E7F9E);
        Ui.rect(g, sx, sy + 10, sw, Math.round(sh * 0.62f) - 10, 0xFF5E7F9E);
        Ui.rect(g, sx, sy + Math.round(sh * 0.62f), sw, Math.round(sh * 0.04f), 0xFF4E7A3A);

        if (module instanceof CrosshairModule crosshair) {
            crosshair.drawAt(g, sx + sw / 2, sy + sh / 2, p.accent());
        } else if (module instanceof HudModule hud) {
            // The HUD's own font at the design's 12px, top-left with 10px inset.
            String text = hud.editorLabel();
            float s = 12f / 9f;
            g.pose().pushMatrix();
            g.pose().translate(sx + 10, sy + 10);
            g.pose().scale(s, s);
            int textW = Minecraft.getInstance().font.width(text);
            if (HudStyleModule.background()) {
                g.fill(-4, -3, textW + 4, 11, 0x8C000000);
            }
            g.drawString(Minecraft.getInstance().font, text, 0, 0, HudStyleModule.textColour(), HudStyleModule.shadow());
            g.pose().popMatrix();
        } else {
            Ui.midCentered(g, Type.S13, Ui.trim(Type.S13, module.displayName(), sw - 16), sx + sw / 2, sy + sh / 2, 0xFFFFFFFF);
        }

        int ny = sy + sh + 12;
        for (String line : note) {
            Ui.text(g, Type.R11, line, x + 17, ny + 2, p.sub());
            ny += 16;
        }
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    public void drag(double mx, double my) {
        if (draggingSlider != null) {
            draggingSlider.setFraction((mx - sliderX) / (double) sliderW);
        }
    }

    @Override
    public void release() {
        draggingSlider = null;
    }

    @Override
    public boolean scroll(double mx, double my, double amount) {
        int step = (int) Math.round(-amount * 40);
        if (optionsFor != null) {
            optionsScroll = Math.max(0, Math.min(optionsMaxScroll, optionsScroll + step));
        } else {
            scroll = Math.max(0, Math.min(maxScroll, scroll + step));
        }
        return true;
    }

    @Override
    public boolean capturingKeys() {
        return listeningToggleKey || listeningSetting != null;
    }

    @Override
    public boolean key(int glfwKey) {
        if (listeningToggleKey && optionsFor != null) {
            int bound = glfwKey == GLFW.GLFW_KEY_ESCAPE ? -1 : glfwKey;
            Keybinds.setKey(optionsFor, bound);
            if (bound > 0) {
                Keybinds.warnConflicts(bound, "toggle:" + optionsFor.id());
            }
            listeningToggleKey = false;
            return true;
        }
        if (listeningSetting != null) {
            int bound = glfwKey == GLFW.GLFW_KEY_ESCAPE ? 0 : glfwKey;
            listeningSetting.set(bound);
            if (bound > 0) {
                Keybinds.warnConflicts(bound, listeningSetting.moduleId() + "." + listeningSetting.key());
            }
            listeningSetting = null;
            return true;
        }
        if (optionsFor != null && (glfwKey == GLFW.GLFW_KEY_ESCAPE || glfwKey == GLFW.GLFW_KEY_BACKSPACE)) {
            optionsFor = null;
            return true;
        }
        if (glfwKey == GLFW.GLFW_KEY_DOWN || glfwKey == GLFW.GLFW_KEY_UP) {
            return scroll(0, 0, glfwKey == GLFW.GLFW_KEY_DOWN ? -1 : 1);
        }
        return false;
    }
}
