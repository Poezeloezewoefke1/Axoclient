package dev.axoclient.gui.menu;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleDescriptions;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.core.ModuleSetting;
import dev.axoclient.gui.render.ModuleIcons;
import dev.axoclient.gui.theme.Palette;
import dev.axoclient.hud.HudModule;
import dev.axoclient.input.Keybinds;
import dev.axoclient.modules.hud.CrosshairModule;
import dev.axoclient.modules.hud.HudStyleModule;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/**
 * The Modules page: filter chips, a card grid or a list, and per-module
 * options.
 *
 * Options are generated from each module's own settings rather than written
 * per module, so all of the client's modules get an options page:
 * an On/Off choice becomes a switch, other choices become a row of mode
 * buttons (or arrows when they don't fit), numbers become sliders, and key
 * settings become a "press a key" button.
 */
final class ModulesPage implements MenuPage {
    private static final String[] CHIPS = {"All", "New", "HUD", "PvP", "QoL", "Performance"};
    private static final int CARD_MIN_W = 112;
    private static final int CARD_H = 92;
    private static final int GAP = 8;
    private static final int LIST_ROW_H = 30;
    private static final int OPTION_ROW_H = 28;

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

    private void open(AxoModule module) {
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
    // Render
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, Palette p, Hits hits, int x, int y, int w, int h, int mx, int my, String query) {
        if (optionsFor != null) {
            renderOptions(g, p, hits, x, y, w, h, mx, my);
            return;
        }
        // Filter chips, then the grid/list switch on the right.
        int cx = x;
        for (int i = 0; i < CHIPS.length; i++) {
            int cw = Draw.chipWidth(CHIPS[i]);
            if (cx + cw > x + w - 52) {
                break;
            }
            Draw.chip(g, CHIPS[i], cx, y, 16, chip == i, p);
            int index = i;
            hits.add(cx, y, cw, 16, () -> {
                chip = index;
                scroll = 0;
            });
            cx += cw + 4;
        }
        boolean list = MenuSettings.listView();
        int tx = x + w - 44;
        Draw.round(g, tx, y, 44, 16, 5, p.raised());
        Draw.round(g, tx + 2, y + 2, 19, 12, 4, list ? p.accentSoft() : 0);
        Draw.round(g, tx + 23, y + 2, 19, 12, 4, list ? 0 : p.accentSoft());
        for (int i = 0; i < 3; i++) {
            g.fill(tx + 7, y + 4 + i * 3, tx + 16, y + 5 + i * 3, list ? p.accent() : p.sub());
        }
        Draw.iconModulesSmall(g, tx + 28, y + 4, list ? p.sub() : p.accent());
        hits.add(tx, y, 22, 16, () -> MenuSettings.setListView(true));
        hits.add(tx + 22, y, 22, 16, () -> MenuSettings.setListView(false));

        int top = y + 24;
        int viewH = h - 24;
        List<AxoModule> modules = visible(query);
        if (modules.isEmpty()) {
            Draw.centered(g, "No modules match your search.", x + w / 2, top + 30, p.sub());
            return;
        }
        g.enableScissor(x, top, x + w, top + viewH);
        hits.clip(x, top, w, viewH);
        int contentH = list ? renderList(g, p, hits, modules, x, top - scroll, w, mx, my)
                            : renderGrid(g, p, hits, modules, x, top - scroll, w, mx, my);
        hits.unclip();
        g.disableScissor();
        maxScroll = Math.max(0, contentH - viewH);
        scroll = Math.min(scroll, maxScroll);
        scrollbar(g, p, x + w + 4, top, viewH, contentH);
    }

    private static void scrollbar(GuiGraphics g, Palette p, int x, int y, int viewH, int contentH) {
        if (contentH <= viewH) {
            return;
        }
        g.fill(x, y, x + 2, y + viewH, p.line());
        int thumbH = Math.max(16, viewH * viewH / contentH);
        g.fill(x, y, x + 2, y + thumbH, p.sub());
    }

    private int renderGrid(
        GuiGraphics g, Palette p, Hits hits, List<AxoModule> modules, int x, int y, int w, int mx, int my
    ) {
        int cols = Math.max(2, Math.min(5, (w + GAP) / (CARD_MIN_W + GAP)));
        int cardW = (w - GAP * (cols - 1)) / cols;
        for (int i = 0; i < modules.size(); i++) {
            int cx = x + (i % cols) * (cardW + GAP);
            int cy = y + (i / cols) * (CARD_H + GAP);
            card(g, p, hits, modules.get(i), cx, cy, cardW, mx, my);
        }
        int rows = (modules.size() + cols - 1) / cols;
        return rows * (CARD_H + GAP) - GAP;
    }

    /** One module card: icon and name, OPTIONS row, and the ENABLED/DISABLED bar. */
    private void card(GuiGraphics g, Palette p, Hits hits, AxoModule module, int x, int y, int w, int mx, int my) {
        boolean on = ModuleManager.get().isEnabled(module);
        boolean hover = Hits.over(mx, my, x, y, w, CARD_H);
        Draw.card(g, x, y, w, CARD_H, 7, p.card(), hover ? p.line() : p.border());

        if (ModuleDescriptions.isNew(module.id())) {
            Draw.round(g, x + 6, y + 6, Draw.width("NEW") + 8, 11, 3, p.accentSoft());
            Draw.text(g, "NEW", x + 10, y + 8, p.accent());
        }
        ModuleIcons.draw(g, module.category(), x, y + 6, w, 30, on ? p.text() : p.muted());
        Draw.centered(g, Draw.trim(module.displayName(), w - 10), x + w / 2, y + 40, on ? p.text() : p.sub());
        int key = Keybinds.keyOf(module);
        if (MenuSettings.keybindsOnCards() && key > 0) {
            Draw.centered(g, "[" + Keybinds.keyName(key) + "]", x + w / 2, y + 50, p.muted());
        }

        // OPTIONS | sliders, then the state bar.
        int rowY = y + CARD_H - 33;
        g.fill(x + 1, rowY, x + w - 1, rowY + 1, p.border());
        g.fill(x + 1, rowY + 1, x + w - 1, rowY + 17, p.raised());
        Draw.centered(g, "OPTIONS", x + (w - 20) / 2, rowY + 5, p.sub());
        g.fill(x + w - 21, rowY + 1, x + w - 20, rowY + 17, p.border());
        Draw.iconSettingsSmall(g, x + w - 15, rowY + 4, p.sub());
        hits.add(x, rowY, w, 17, () -> open(module));

        int barY = rowY + 17;
        int barH = y + CARD_H - 1 - barY;
        int barColour = on ? p.accent() : p.raised();
        g.fill(x + 1, barY, x + w - 1, barY + 1, p.border());
        // Rounded at the bottom to match the card, square where it meets the row above.
        Draw.round(g, x + 1, barY + 1, w - 2, barH - 1, 6, barColour);
        g.fill(x + 1, barY + 1, x + w - 1, barY + 7, barColour);
        Draw.boldCentered(g, on ? "ENABLED" : "DISABLED", x + w / 2, barY + (barH - 7) / 2, on ? p.onAccent() : p.sub());
        hits.add(x, barY, w, CARD_H - (barY - y), () -> ModuleManager.get().toggle(module));
    }

    private int renderList(
        GuiGraphics g, Palette p, Hits hits, List<AxoModule> modules, int x, int y, int w, int mx, int my
    ) {
        for (int i = 0; i < modules.size(); i++) {
            AxoModule module = modules.get(i);
            int ry = y + i * (LIST_ROW_H + 5);
            boolean on = ModuleManager.get().isEnabled(module);
            Draw.card(g, x, ry, w, LIST_ROW_H, 6, p.card(), p.border());
            Draw.round(g, x + 6, ry + 4, 22, 22, 5, p.raised());
            ModuleIcons.drawSmall(g, module.category(), x + 6, ry + 4, 22, 22, on ? p.text() : p.muted());

            int textW = w - 150;
            Draw.text(g, Draw.trim(module.displayName(), textW), x + 34, ry + 5, p.text());
            Draw.text(g, Draw.trim(module.description(), textW), x + 34, ry + 16, p.muted());
            String cat = module.category().displayName().toUpperCase(Locale.ROOT);
            Draw.text(g, Draw.trim(cat, 70), x + w - 112, ry + 11, p.muted());

            // Chevron to options, then the switch.
            int chevX = x + w - 40;
            for (int k = 0; k < 4; k++) {
                g.fill(chevX + k, ry + 11 + k, chevX + k + 1, ry + 12 + k, p.sub());
                g.fill(chevX + k, ry + 18 - k, chevX + k + 1, ry + 19 - k, p.sub());
            }
            hits.add(chevX - 6, ry, 16, LIST_ROW_H, () -> open(module));
            hits.add(x, ry, w - 46, LIST_ROW_H, () -> open(module));
            Draw.toggle(g, x + w - 28, ry + 9, 22, 12, on, p);
            hits.add(x + w - 30, ry, 30, LIST_ROW_H, () -> ModuleManager.get().toggle(module));
        }
        return modules.size() * (LIST_ROW_H + 5) - 5;
    }

    // ------------------------------------------------------------------
    // Options
    // ------------------------------------------------------------------

    private void renderOptions(GuiGraphics g, Palette p, Hits hits, int x, int y, int w, int h, int mx, int my) {
        AxoModule module = optionsFor;
        int previewW = Math.max(120, Math.min(170, w * 3 / 10));
        int leftW = w - previewW - 10;
        boolean on = ModuleManager.get().isEnabled(module);

        // Header card: back, icon, name and description, keybind, switch.
        Draw.card(g, x, y, leftW, 38, 7, p.card(), p.border());
        Draw.card(g, x + 7, y + 10, 18, 18, 5, p.raised(), p.border());
        for (int k = 0; k < 4; k++) {
            g.fill(x + 17 - k, y + 15 + k, x + 18 - k, y + 16 + k, p.text());
            g.fill(x + 17 - k, y + 22 - k, x + 18 - k, y + 23 - k, p.text());
        }
        hits.add(x + 7, y + 10, 18, 18, () -> optionsFor = null);
        Draw.round(g, x + 30, y + 7, 24, 24, 6, p.accentSoft());
        ModuleIcons.drawSmall(g, module.category(), x + 30, y + 7, 24, 24, p.accent());

        int right = x + leftW - 8;
        Draw.toggle(g, right - 24, y + 19, 24, 13, on, p);
        Draw.label(g, on ? "Enabled" : "Disabled", right - Draw.width(on ? "ENABLED" : "DISABLED"), y + 7, p.muted());
        hits.add(right - 26, y + 16, 28, 18, () -> ModuleManager.get().toggle(module));

        String keyLabel = listeningToggleKey ? "PRESS A KEY" : keyText(Keybinds.keyOf(module));
        int keyW = Math.max(44, Draw.width(keyLabel) + 12);
        int keyX = right - 34 - keyW;
        Draw.label(g, "Keybind", keyX + keyW - Draw.width("KEYBIND"), y + 7, p.muted());
        Draw.card(g, keyX, y + 17, keyW, 15, 4, p.raised(), listeningToggleKey ? p.accent() : p.border());
        Draw.centered(g, keyLabel, keyX + keyW / 2, y + 21, listeningToggleKey ? p.accent() : p.text());
        hits.add(keyX, y + 17, keyW, 15, () -> {
            listeningToggleKey = true;
            listeningSetting = null;
        });

        int nameW = keyX - (x + 60) - 6;
        Draw.text(g, Draw.trim(module.displayName(), nameW), x + 60, y + 9, p.text());
        Draw.text(g, Draw.trim(module.description(), nameW), x + 60, y + 21, p.sub());

        // Setting rows, scrolling, then Reset to defaults.
        int top = y + 44;
        int viewH = h - 44;
        List<ModuleSetting> settings = module.settings();
        g.enableScissor(x, top, x + leftW, top + viewH);
        hits.clip(x, top, leftW, viewH);
        int ry = top - optionsScroll;
        if (settings.isEmpty()) {
            Draw.card(g, x, ry, leftW, OPTION_ROW_H, 6, p.card(), p.border());
            Draw.text(g, "This module has nothing to adjust.", x + 10, ry + 10, p.sub());
            ry += OPTION_ROW_H + 5;
        }
        for (ModuleSetting setting : settings) {
            Draw.card(g, x, ry, leftW, OPTION_ROW_H, 6, p.card(), p.border());
            int controlW = renderControl(g, p, hits, setting, x + leftW - 8, ry, mx, my);
            Draw.text(g, Draw.trim(setting.label(), leftW - controlW - 24), x + 10, ry + 10, p.text());
            ry += OPTION_ROW_H + 5;
        }
        if (!settings.isEmpty()) {
            String reset = "Reset to defaults";
            int rw = Draw.width(reset) + 16;
            Draw.card(g, x + leftW - rw, ry + 2, rw, 16, 5, p.panel(), p.border());
            Draw.centered(g, reset, x + leftW - rw / 2, ry + 6, p.sub());
            hits.add(x + leftW - rw, ry + 2, rw, 16, () -> settings.forEach(ModuleSetting::reset));
            ry += 22;
        }
        hits.unclip();
        g.disableScissor();
        int contentH = ry + optionsScroll - top;
        optionsMaxScroll = Math.max(0, contentH - viewH);
        optionsScroll = Math.min(optionsScroll, optionsMaxScroll);

        renderPreview(g, p, module, x + leftW + 10, y, previewW, h);
    }

    private static String keyText(int key) {
        return key > 0 ? Keybinds.keyName(key).toUpperCase(Locale.ROOT) : "NONE";
    }

    /**
     * Draws the control for one setting, right-aligned at {@code right}, and
     * registers its clicks. Returns the width it used.
     */
    private int renderControl(GuiGraphics g, Palette p, Hits hits, ModuleSetting setting, int right, int y, int mx, int my) {
        if (setting.isSwitch()) {
            boolean on = setting.switchOn();
            Draw.toggle(g, right - 24, y + 8, 24, 13, on, p);
            hits.add(right - 28, y, 32, OPTION_ROW_H, () -> setting.setSwitch(!setting.switchOn()));
            return 24;
        }
        switch (setting.format()) {
            case CHOICE -> {
                List<String> choices = setting.choices();
                int segW = Draw.segmentedWidth(choices, 5);
                if (segW <= 230) {
                    int sx = right - segW;
                    int[] edges = Draw.segmented(g, sx, y + 6, 16, choices, setting.get(), p, 5);
                    for (int i = 0; i < choices.size(); i++) {
                        int index = i;
                        hits.add(edges[i], y + 6, edges[i + 1] - edges[i], 16, () -> setting.set(index));
                    }
                    return segW;
                }
                // Too many to show at once: arrows either side of the current one.
                int cw = 120;
                int cx = right - cw;
                Draw.round(g, cx, y + 6, cw, 16, 5, p.raised());
                Draw.centered(g, "<", cx + 8, y + 10, p.sub());
                Draw.centered(g, ">", cx + cw - 8, y + 10, p.sub());
                Draw.centered(g, Draw.trim(setting.display(), cw - 30), cx + cw / 2, y + 10, p.text());
                hits.add(cx, y + 6, cw / 2, 16, () -> setting.nudge(-1));
                hits.add(cx + cw / 2, y + 6, cw / 2, 16, () -> setting.nudge(1));
                return cw;
            }
            case KEY -> {
                boolean listening = listeningSetting == setting;
                String label = listening ? "PRESS A KEY" : keyText(setting.get());
                int kw = Math.max(50, Draw.width(label) + 14);
                Draw.card(g, right - kw, y + 6, kw, 16, 4, p.raised(), listening ? p.accent() : p.border());
                Draw.centered(g, label, right - kw / 2, y + 10, listening ? p.accent() : p.text());
                hits.add(right - kw, y + 6, kw, 16, () -> {
                    listeningSetting = setting;
                    listeningToggleKey = false;
                });
                return kw;
            }
            default -> {
                // A slider plus its value box.
                int boxW = 38;
                int trackW = 90;
                int tx = right - boxW - 8 - trackW;
                int ty = y + 13;
                g.fill(tx, ty, tx + trackW, ty + 3, p.off());
                int filled = (int) Math.round(setting.fraction() * trackW);
                g.fill(tx, ty, tx + filled, ty + 3, p.accent());
                Draw.round(g, tx + filled - 4, ty - 3, 9, 9, 4, 0xFFFFFFFF);
                Draw.round(g, right - boxW, y + 6, boxW, 16, 4, p.raised());
                Draw.centered(g, Draw.trim(setting.display(), boxW - 4), right - boxW / 2, y + 10, p.text());
                hits.add(tx - 4, y + 4, trackW + 8, 20, () -> {
                    draggingSlider = setting;
                    sliderX = tx;
                    sliderW = trackW;
                    setting.setFraction((mx - tx) / (double) trackW);
                });
                return boxW + 8 + trackW;
            }
        }
    }

    /** The live preview: a little scene with the module's real output on it. */
    private static void renderPreview(GuiGraphics g, Palette p, AxoModule module, int x, int y, int w, int h) {
        Draw.card(g, x, y, w, h, 8, p.card(), p.border());
        Draw.label(g, "Live preview", x + 9, y + 9, p.muted());

        List<String> note = Draw.wrap(module.description(), w - 18);
        int noteH = note.size() * 10;
        int sx = x + 8;
        int sy = y + 22;
        int sw = w - 16;
        int sh = Math.max(40, h - 22 - noteH - 16);
        // Sky, a strip of grass, dirt: a stand-in for the world behind the HUD.
        Draw.round(g, sx, sy, sw, sh, 5, 0xFF5E7F9E);
        int grassY = sy + sh * 62 / 100;
        g.fill(sx, grassY, sx + sw, grassY + Math.max(2, sh / 25), 0xFF4E7A3A);
        Draw.round(g, sx, grassY + Math.max(2, sh / 25), sw, sy + sh - grassY - Math.max(2, sh / 25), 5, 0xFF6B5236);
        g.fill(sx, grassY + Math.max(2, sh / 25), sx + sw, grassY + Math.max(2, sh / 25) + 5, 0xFF6B5236);

        if (module instanceof CrosshairModule crosshair) {
            crosshair.drawAt(g, sx + sw / 2, sy + sh / 2, p.accent());
        } else if (module instanceof HudModule hud) {
            String text = Draw.trim(hud.editorLabel(), sw - 16);
            int tx = sx + 7;
            int ty = sy + 7;
            if (HudStyleModule.background()) {
                g.fill(tx - 2, ty - 2, tx + Draw.width(text) + 2, ty + 9, 0x80000000);
            }
            g.drawString(Draw.font(), text, tx, ty, HudStyleModule.textColour(), HudStyleModule.shadow());
        } else {
            Draw.centered(g, Draw.trim(module.displayName(), sw - 8), sx + sw / 2, sy + sh / 2 - 4, 0xFFFFFFFF);
        }

        int ny = sy + sh + 8;
        for (String line : note) {
            Draw.text(g, line, x + 9, ny, p.sub());
            ny += 10;
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
        int step = (int) Math.round(-amount * 24);
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

    /** Opens a module's options directly, for links from other pages. */
    void openOptions(AxoModule module) {
        open(module);
    }
}
