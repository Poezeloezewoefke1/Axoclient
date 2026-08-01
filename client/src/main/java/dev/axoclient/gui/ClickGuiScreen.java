package dev.axoclient.gui;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.core.ModuleSetting;
import dev.axoclient.gui.render.GuiRender;
import dev.axoclient.gui.render.ModuleIcons;
import dev.axoclient.gui.theme.GuiTheme;
import dev.axoclient.gui.theme.Themes;
import dev.axoclient.input.Keybinds;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * The Axo mod menu: a grid of module cards with category tabs, a search box
 * and per-module favourites.
 *
 * <h2>Why a grid and not the old panels</h2>
 * The previous menu was a set of draggable per-category panels holding dense
 * text rows. That does not scale: with fifty-odd modules the panels overlap,
 * and every module looks identical, so finding one means reading every line.
 * A card carries a pictogram, a name and its state at a glance, and a fixed
 * grid means nothing can end up underneath anything else.
 *
 * <h2>Scrolling</h2>
 * The grid scrolls with a dragged scrollbar and the arrow/page keys rather
 * than the mouse wheel. This screen already uses 1.21.11's event-object input
 * API, and the wheel callback is the one shape not exercised anywhere else in
 * the codebase — so it is left alone rather than guessed at.
 */
public final class ClickGuiScreen extends Screen {
    private static final int MARGIN = 14;
    private static final int TOPBAR_H = 26;
    private static final int TABS_H = 22;
    private static final int CARD_W = 132;
    private static final int CARD_H = 118;
    private static final int CARD_GAP = 10;
    private static final int SCROLLBAR_W = 4;
    /** Rows moved per arrow key press. */
    private static final int KEY_SCROLL = 24;

    /** null = the "All" tab. */
    private ModuleCategory activeTab;
    private boolean favouritesOnly;
    private String search = "";
    private boolean searchFocused;

    private int scroll;
    private int maxScroll;
    private boolean draggingScrollbar;

    /** Module whose settings overlay is open, or null. */
    private AxoModule settingsFor;
    /** Setting waiting for a key press to rebind, or null. */
    private ModuleSetting awaitingBind;
    /** Module waiting for a key press to set its toggle key, or null. */
    private AxoModule awaitingModuleBind;

    public ClickGuiScreen() {
        super(Component.literal("Axo Mod Menu"));
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private int gridTop() {
        return MARGIN + TOPBAR_H + TABS_H + 8;
    }

    private int columns() {
        int usable = width - MARGIN * 2 - SCROLLBAR_W - 4;
        return Math.max(1, (usable + CARD_GAP) / (CARD_W + CARD_GAP));
    }

    /** Modules matching the current tab, search and favourites filter. */
    private List<AxoModule> visibleModules() {
        String needle = search.toLowerCase(Locale.ROOT).trim();
        List<AxoModule> out = new ArrayList<>();
        for (AxoModule module : ModuleManager.get().all()) {
            if (activeTab != null && module.category() != activeTab) {
                continue;
            }
            if (favouritesOnly && !isFavourite(module)) {
                continue;
            }
            if (!needle.isEmpty() && !module.displayName().toLowerCase(Locale.ROOT).contains(needle)) {
                continue;
            }
            out.add(module);
        }
        // Favourites first, so starring something moves it where you can see it.
        out.sort((a, b) -> {
            int fav = Boolean.compare(isFavourite(b), isFavourite(a));
            return fav != 0 ? fav : a.displayName().compareToIgnoreCase(b.displayName());
        });
        return out;
    }

    private int[] cardBounds(int index) {
        int cols = columns();
        int col = index % cols;
        int row = index / cols;
        return new int[] {
            MARGIN + col * (CARD_W + CARD_GAP),
            gridTop() + row * (CARD_H + CARD_GAP) - scroll,
            CARD_W,
            CARD_H
        };
    }

    private static boolean isFavourite(AxoModule module) {
        return ModuleManager.get().config().getModuleBool(module.id(), "favourite", false);
    }

    private static void setFavourite(AxoModule module, boolean value) {
        ModuleManager.get().config().setModuleBool(module.id(), "favourite", value);
    }

    // ------------------------------------------------------------------
    // Render
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        GuiTheme theme = Themes.current();
        GuiRender.rect(g, 0, 0, width, height, theme.dark ? 0xC8000000 : 0x80000000);

        renderTopBar(g, theme, mouseX, mouseY);
        renderTabs(g, theme, mouseX, mouseY);
        renderGrid(g, theme, mouseX, mouseY);

        if (settingsFor != null) {
            renderSettings(g, theme, mouseX, mouseY);
        }
        super.render(g, mouseX, mouseY, delta);
    }

    private void renderTopBar(GuiGraphics g, GuiTheme theme, int mouseX, int mouseY) {
        int y = MARGIN;
        // Title pill, matching the accent so it reads as the brand mark.
        int titleW = GuiRender.textWidth("AXO MOD MENU") + 24;
        GuiRender.rect(g, MARGIN, y, titleW, 22, theme.accent);
        GuiRender.roundCorners(g, MARGIN, y, titleW, 22);
        GuiRender.text(g, "AXO MOD MENU", MARGIN + 12, y + 7, 0xFFFFFFFF);

        // Search box, right-aligned.
        int boxW = 190;
        int boxX = width - MARGIN - boxW;
        boolean hover = GuiRender.inside(mouseX, mouseY, boxX, y, boxW, 22);
        GuiRender.rect(g, boxX, y, boxW, 22, theme.panel);
        GuiRender.outline(g, boxX, y, boxW, 22, searchFocused ? theme.accent : theme.outline);
        GuiRender.roundCorners(g, boxX, y, boxW, 22);
        String shown = search.isEmpty() && !searchFocused ? "Search modules..." : search;
        int textColour = search.isEmpty() && !searchFocused ? theme.textDim : theme.text;
        GuiRender.text(g, shown, boxX + 8, y + 7, textColour);
        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            GuiRender.rect(g, boxX + 8 + GuiRender.textWidth(search) + 1, y + 6, 1, 10, theme.text);
        }
        if (hover && !search.isEmpty()) {
            GuiRender.text(g, "x", boxX + boxW - 12, y + 7, theme.textDim);
        }

        // Favourites filter, left of the search box.
        int starX = boxX - 30;
        GuiRender.rect(g, starX, y, 22, 22, favouritesOnly ? theme.accent : theme.panel);
        GuiRender.outline(g, starX, y, 22, 22, theme.outline);
        GuiRender.roundCorners(g, starX, y, 22, 22);
        ModuleIcons.star(g, starX + 11, y + 11, 9, favouritesOnly, favouritesOnly ? 0xFFFFFFFF : theme.textDim);
    }

    private void renderTabs(GuiGraphics g, GuiTheme theme, int mouseX, int mouseY) {
        int x = MARGIN;
        int y = MARGIN + TOPBAR_H;
        for (int i = 0; i <= ModuleCategory.values().length; i++) {
            ModuleCategory category = i == 0 ? null : ModuleCategory.values()[i - 1];
            String label = i == 0 ? "All" : category.displayName();
            int w = GuiRender.textWidth(label) + 22;
            boolean active = activeTab == category;
            boolean hover = GuiRender.inside(mouseX, mouseY, x, y, w, 18);

            int fill = active ? theme.accent : (hover ? theme.hover : theme.panel);
            GuiRender.rect(g, x, y, w, 18, fill);
            GuiRender.roundCorners(g, x, y, w, 18);
            GuiRender.text(g, label, x + 11, y + 5, active ? 0xFFFFFFFF : theme.text);
            x += w + 6;
        }
    }

    private void renderGrid(GuiGraphics g, GuiTheme theme, int mouseX, int mouseY) {
        List<AxoModule> modules = visibleModules();
        int top = gridTop();
        int viewportH = height - top - MARGIN;

        int rows = (int) Math.ceil(modules.size() / (double) columns());
        int contentH = Math.max(0, rows * (CARD_H + CARD_GAP) - CARD_GAP);
        maxScroll = Math.max(0, contentH - viewportH);
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        if (modules.isEmpty()) {
            GuiRender.centered(g, "No modules match \"" + search + "\"", width / 2, top + 40, theme.textDim);
            return;
        }

        for (int i = 0; i < modules.size(); i++) {
            int[] b = cardBounds(i);
            // Cull rows scrolled out of view; the grid can be a few hundred
            // cards tall and only a screenful is ever visible.
            if (b[1] + CARD_H < top || b[1] > height) {
                continue;
            }
            renderCard(g, theme, modules.get(i), b[0], b[1], mouseX, mouseY);
        }

        if (maxScroll > 0) {
            int trackX = width - MARGIN + 4;
            GuiRender.rect(g, trackX, top, SCROLLBAR_W, viewportH, theme.outline);
            int thumbH = Math.max(24, (int) (viewportH * (viewportH / (double) contentH)));
            int thumbY = top + (int) ((viewportH - thumbH) * (scroll / (double) maxScroll));
            GuiRender.rect(g, trackX, thumbY, SCROLLBAR_W, thumbH, theme.accent);
        }
    }

    private void renderCard(
        GuiGraphics g, GuiTheme theme, AxoModule module, int x, int y, int mouseX, int mouseY
    ) {
        boolean enabled = ModuleManager.get().isEnabled(module);
        boolean hover = GuiRender.inside(mouseX, mouseY, x, y, CARD_W, CARD_H);

        GuiRender.rect(g, x, y, CARD_W, CARD_H, hover ? theme.hover : theme.panel);
        GuiRender.outline(g, x, y, CARD_W, CARD_H, enabled ? theme.accent : theme.outline);
        GuiRender.roundCorners(g, x, y, CARD_W, CARD_H);

        // Name, trimmed to fit beside the star.
        String name = module.displayName();
        int nameMax = CARD_W - 34;
        while (GuiRender.textWidth(name) > nameMax && name.length() > 1) {
            name = name.substring(0, name.length() - 1);
        }
        if (!name.equals(module.displayName())) {
            name = name.substring(0, Math.max(1, name.length() - 1)) + "...";
        }
        GuiRender.text(g, name, x + 10, y + 10, theme.text);

        boolean favourite = isFavourite(module);
        ModuleIcons.star(g, x + CARD_W - 15, y + 14, 9, favourite, favourite ? theme.accent : theme.textDim);

        // Toggle key, bound by right-clicking the card.
        String bind = awaitingModuleBind == module ? "press a key" : Keybinds.keyName(Keybinds.keyOf(module));
        if (!bind.isEmpty()) {
            GuiRender.text(
                g, "[" + bind + "]", x + 10, y + 22,
                awaitingModuleBind == module ? theme.accent : theme.textDim
            );
        }

        ModuleIcons.draw(g, module.category(), x, y + 26, CARD_W, 46, enabled ? theme.accent : theme.textDim);

        // Bottom row: gear button then the state button.
        int rowY = y + CARD_H - 30;
        int gearW = 26;
        boolean gearHover = GuiRender.inside(mouseX, mouseY, x + 8, rowY, gearW, 20);
        GuiRender.rect(g, x + 8, rowY, gearW, 20, gearHover ? theme.accent : theme.moduleOff);
        GuiRender.roundCorners(g, x + 8, rowY, gearW, 20);
        boolean hasSettings = !module.settings().isEmpty();
        ModuleIcons.gear(
            g, x + 8 + gearW / 2, rowY + 10, 4,
            hasSettings ? (gearHover ? 0xFFFFFFFF : theme.text) : theme.textDim
        );

        int stateX = x + 8 + gearW + 6;
        int stateW = CARD_W - (stateX - x) - 8;
        boolean stateHover = GuiRender.inside(mouseX, mouseY, stateX, rowY, stateW, 20);
        int stateFill = enabled ? theme.moduleOn : theme.moduleOff;
        if (stateHover) {
            stateFill = GuiRender.blend(stateFill, 0xFFFFFFFF, 0.12f);
        }
        GuiRender.rect(g, stateX, rowY, stateW, 20, stateFill);
        GuiRender.roundCorners(g, stateX, rowY, stateW, 20);
        GuiRender.centered(
            g, enabled ? "Enabled" : "Disabled", stateX + stateW / 2, rowY + 6,
            enabled ? 0xFFFFFFFF : theme.textDim
        );
    }

    // ------------------------------------------------------------------
    // Settings overlay
    // ------------------------------------------------------------------

    private int settingsRowHeight() {
        return 22;
    }

    private int[] settingsPanelBounds() {
        List<ModuleSetting> settings = settingsFor.settings();
        int w = 240;
        int h = 44 + Math.max(1, settings.size()) * settingsRowHeight() + 10;
        return new int[] {(width - w) / 2, (height - h) / 2, w, h};
    }

    private void renderSettings(GuiGraphics g, GuiTheme theme, int mouseX, int mouseY) {
        GuiRender.rect(g, 0, 0, width, height, 0x99000000);
        int[] p = settingsPanelBounds();
        GuiRender.panel(g, p[0], p[1], p[2], p[3], theme);
        GuiRender.text(g, settingsFor.displayName(), p[0] + 12, p[1] + 12, theme.text);
        GuiRender.text(g, "Done", p[0] + p[2] - 12 - GuiRender.textWidth("Done"), p[1] + 12, theme.accent);

        List<ModuleSetting> settings = settingsFor.settings();
        if (settings.isEmpty()) {
            GuiRender.text(g, "No settings for this module.", p[0] + 12, p[1] + 40, theme.textDim);
            return;
        }
        int y = p[1] + 36;
        for (ModuleSetting setting : settings) {
            boolean binding = awaitingBind == setting;
            GuiRender.text(g, setting.label(), p[0] + 12, y + 6, theme.textDim);

            String value = binding ? "press a key..." : setting.display();
            int valueW = Math.max(70, GuiRender.textWidth(value) + 28);
            int valueX = p[0] + p[2] - 12 - valueW;
            GuiRender.rect(g, valueX, y, valueW, 18, theme.moduleOff);
            GuiRender.roundCorners(g, valueX, y, valueW, 18);
            GuiRender.centered(g, value, valueX + valueW / 2, y + 5, binding ? theme.accent : theme.text);

            // Steppers, except for keys — those are set by pressing one.
            if (setting.format() != ModuleSetting.Format.KEY) {
                GuiRender.centered(g, "-", valueX + 8, y + 5, theme.textDim);
                GuiRender.centered(g, "+", valueX + valueW - 8, y + 5, theme.textDim);
            }
            y += settingsRowHeight();
        }
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int button = event.buttonInfo().button();

        if (settingsFor != null) {
            return settingsClick(mx, my, button);
        }

        searchFocused = false;

        // Search box
        int boxW = 190;
        int boxX = width - MARGIN - boxW;
        if (GuiRender.inside(mx, my, boxX, MARGIN, boxW, 22)) {
            if (!search.isEmpty() && mx >= boxX + boxW - 16) {
                search = "";
            } else {
                searchFocused = true;
            }
            return true;
        }
        // Favourites filter
        if (GuiRender.inside(mx, my, boxX - 30, MARGIN, 22, 22)) {
            favouritesOnly = !favouritesOnly;
            scroll = 0;
            return true;
        }
        // Tabs
        int x = MARGIN;
        int tabY = MARGIN + TOPBAR_H;
        for (int i = 0; i <= ModuleCategory.values().length; i++) {
            ModuleCategory category = i == 0 ? null : ModuleCategory.values()[i - 1];
            String label = i == 0 ? "All" : category.displayName();
            int w = GuiRender.textWidth(label) + 22;
            if (GuiRender.inside(mx, my, x, tabY, w, 18)) {
                activeTab = category;
                scroll = 0;
                return true;
            }
            x += w + 6;
        }
        // Scrollbar
        if (maxScroll > 0 && GuiRender.inside(mx, my, width - MARGIN + 4, gridTop(), SCROLLBAR_W, height - gridTop())) {
            draggingScrollbar = true;
            dragScrollTo(my);
            return true;
        }
        // Cards
        List<AxoModule> modules = visibleModules();
        for (int i = 0; i < modules.size(); i++) {
            int[] b = cardBounds(i);
            if (!GuiRender.inside(mx, my, b[0], b[1], CARD_W, CARD_H)) {
                continue;
            }
            AxoModule module = modules.get(i);
            int rowY = b[1] + CARD_H - 30;

            // Right-click anywhere on a card rebinds its toggle key, which is
            // where the old menu put this too.
            if (button == 1) {
                awaitingModuleBind = module;
                return true;
            }
            if (GuiRender.inside(mx, my, b[0] + CARD_W - 24, b[1] + 5, 20, 18)) {
                setFavourite(module, !isFavourite(module));
                return true;
            }
            if (GuiRender.inside(mx, my, b[0] + 8, rowY, 26, 20)) {
                settingsFor = module;
                awaitingBind = null;
                return true;
            }
            int stateX = b[0] + 40;
            if (GuiRender.inside(mx, my, stateX, rowY, CARD_W - 48, 20)) {
                ModuleManager.get().toggle(module);
                return true;
            }
            // Anywhere else on the card toggles too — the card is the button.
            ModuleManager.get().toggle(module);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean settingsClick(double mx, double my, int button) {
        int[] p = settingsPanelBounds();
        if (GuiRender.inside(mx, my, p[0] + p[2] - 44, p[1] + 6, 40, 18)) {
            settingsFor = null;
            awaitingBind = null;
            return true;
        }
        if (!GuiRender.inside(mx, my, p[0], p[1], p[2], p[3])) {
            settingsFor = null;
            awaitingBind = null;
            return true;
        }
        int y = p[1] + 36;
        for (ModuleSetting setting : settingsFor.settings()) {
            String value = setting.display();
            int valueW = Math.max(70, GuiRender.textWidth(value) + 28);
            int valueX = p[0] + p[2] - 12 - valueW;
            if (GuiRender.inside(mx, my, valueX, y, valueW, 18)) {
                if (setting.format() == ModuleSetting.Format.KEY) {
                    awaitingBind = setting;
                } else if (mx < valueX + 16) {
                    setting.nudge(-1);
                } else if (mx > valueX + valueW - 16) {
                    setting.nudge(1);
                } else {
                    // The middle of a choice cycles forward, which is what
                    // people try first on a picker.
                    setting.nudge(button == 1 ? -1 : 1);
                }
                return true;
            }
            y += settingsRowHeight();
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingScrollbar) {
            dragScrollTo(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingScrollbar = false;
        return super.mouseReleased(event);
    }

    private void dragScrollTo(double my) {
        int top = gridTop();
        int viewportH = height - top - MARGIN;
        double t = (my - top) / Math.max(1, viewportH);
        scroll = (int) Math.round(Math.max(0, Math.min(1, t)) * maxScroll);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();

        if (awaitingBind != null) {
            awaitingBind.set(keyCode == GLFW.GLFW_KEY_ESCAPE ? 0 : keyCode);
            awaitingBind = null;
            return true;
        }
        if (awaitingModuleBind != null) {
            Keybinds.setKey(awaitingModuleBind, keyCode == GLFW.GLFW_KEY_ESCAPE ? -1 : keyCode);
            awaitingModuleBind = null;
            return true;
        }
        if (settingsFor != null && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            settingsFor = null;
            return true;
        }
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!search.isEmpty()) {
                    search = search.substring(0, search.length() - 1);
                    scroll = 0;
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_DOWN -> {
                scroll = Math.min(maxScroll, scroll + KEY_SCROLL);
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                scroll = Math.max(0, scroll - KEY_SCROLL);
                return true;
            }
            case GLFW.GLFW_KEY_PAGE_DOWN -> {
                scroll = Math.min(maxScroll, scroll + CARD_H + CARD_GAP);
                return true;
            }
            case GLFW.GLFW_KEY_PAGE_UP -> {
                scroll = Math.max(0, scroll - CARD_H - CARD_GAP);
                return true;
            }
            default -> {
                // fall through to the default screen handling below
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchFocused && event.codepoint() >= ' ') {
            search += event.codepointAsString();
            scroll = 0;
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
