package dev.axoclient.gui;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.gui.render.GuiRender;
import dev.axoclient.gui.theme.GuiTheme;
import dev.axoclient.gui.theme.Themes;
import dev.axoclient.input.Keybinds;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * The Axo ClickGUI: draggable, theme-aware category panels with module
 * toggles, a live search filter, an accent-colour picker, a light/dark
 * switch, and right-click keybinding. Flat-modern styling via GuiRender.
 * Panel positions persist per category in config.
 */
public final class ClickGuiScreen extends Screen {
    private static final int PANEL_W = 122;
    private static final int HEADER_H = 20;
    private static final int ROW_H = 15;
    private static final int TOPBAR_Y = 8;
    private static final int SWATCH = 14;

    private final Map<ModuleCategory, int[]> panelPos = new EnumMap<>(ModuleCategory.class);
    private ModuleCategory dragging;
    private int dragOffX;
    private int dragOffY;
    private String search = "";
    private boolean searchFocused;
    private AxoModule awaitingBind;

    public ClickGuiScreen() {
        super(Component.literal("Axo Client"));
    }

    @Override
    protected void init() {
        int i = 0;
        for (ModuleCategory cat : ModuleCategory.values()) {
            if (modulesIn(cat).isEmpty()) {
                continue;
            }
            int defX = 12 + i * (PANEL_W + 12);
            int defY = 34;
            int x = ModuleManager.get().config().getModuleInt(key(cat), "x", defX);
            int y = ModuleManager.get().config().getModuleInt(key(cat), "y", defY);
            panelPos.put(cat, new int[] { x, y });
            i++;
        }
    }

    private static String key(ModuleCategory cat) {
        return "panel_" + cat.name();
    }

    private List<AxoModule> modulesIn(ModuleCategory cat) {
        List<AxoModule> out = new ArrayList<>();
        for (AxoModule m : ModuleManager.get().all()) {
            if (m.category() == cat && matchesSearch(m)) {
                out.add(m);
            }
        }
        return out;
    }

    private boolean matchesSearch(AxoModule m) {
        if (search.isEmpty()) {
            return true;
        }
        return m.displayName().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        GuiTheme theme = Themes.current();
        g.fill(0, 0, this.width, this.height, theme.background);

        renderTopBar(g, mouseX, mouseY, theme);

        for (Map.Entry<ModuleCategory, int[]> e : panelPos.entrySet()) {
            renderPanel(g, e.getKey(), e.getValue()[0], e.getValue()[1], mouseX, mouseY, theme);
        }

        if (awaitingBind != null) {
            GuiRender.centered(
                g,
                "Press a key to bind " + awaitingBind.displayName() + "  (Esc to clear)",
                this.width / 2,
                this.height - 16,
                theme.accent
            );
        }
        // No super.render: we draw our own themed background above and use no
        // vanilla widgets, so calling it would paint the default backdrop over us.
    }

    private void renderTopBar(GuiGraphics g, int mouseX, int mouseY, GuiTheme theme) {
        // Search box
        int sx = 12;
        int sw = 170;
        GuiRender.rect(g, sx, TOPBAR_Y, sw, 16, theme.panel);
        GuiRender.outline(g, sx, TOPBAR_Y, sw, 16, searchFocused ? theme.accent : theme.outline);
        String shown = search.isEmpty() ? "Search modules…" : search;
        GuiRender.text(g, shown, sx + 6, TOPBAR_Y + 4, search.isEmpty() ? theme.textDim : theme.text);

        // Light/dark toggle
        int tx = sx + sw + 10;
        GuiRender.rect(g, tx, TOPBAR_Y, 44, 16, theme.panel);
        GuiRender.outline(g, tx, TOPBAR_Y, 44, 16, theme.outline);
        GuiRender.centered(g, theme.dark ? "Dark" : "Light", tx + 22, TOPBAR_Y + 4, theme.text);

        // Accent swatches
        int ax = tx + 54;
        for (int i = 0; i < Themes.ACCENT_SWATCHES.length; i++) {
            int x = ax + i * (SWATCH + 4);
            int c = Themes.ACCENT_SWATCHES[i];
            GuiRender.rect(g, x, TOPBAR_Y + 1, SWATCH, SWATCH, c);
            if (c == theme.accent) {
                GuiRender.outline(g, x - 1, TOPBAR_Y, SWATCH + 2, SWATCH + 2, theme.text);
            }
        }
    }

    private void renderPanel(
        GuiGraphics g,
        ModuleCategory cat,
        int x,
        int y,
        int mouseX,
        int mouseY,
        GuiTheme theme
    ) {
        List<AxoModule> mods = modulesIn(cat);
        int height = HEADER_H + mods.size() * ROW_H;
        GuiRender.panel(g, x, y, PANEL_W, height, theme);
        GuiRender.text(g, cat.displayName(), x + 8, y + 6, theme.text);

        int ry = y + HEADER_H;
        for (AxoModule m : mods) {
            boolean on = ModuleManager.get().isEnabled(m);
            boolean hover = GuiRender.inside(mouseX, mouseY, x, ry, PANEL_W, ROW_H);
            if (hover) {
                GuiRender.rect(g, x, ry, PANEL_W, ROW_H, theme.hover);
            }
            if (on) {
                GuiRender.rect(g, x, ry, 2, ROW_H, theme.accent);
            }
            GuiRender.text(g, m.displayName(), x + 8, ry + 4, on ? theme.accent : theme.textDim);

            int bind = Keybinds.keyOf(m);
            String badge = bind > 0 ? Keybinds.keyName(bind) : "";
            if (!badge.isEmpty()) {
                GuiRender.text(g, badge, x + PANEL_W - 8 - GuiRender.textWidth(badge), ry + 4, theme.textDim);
            }
            ry += ROW_H;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int button = event.buttonInfo().button();
        // Search box
        if (GuiRender.inside(mx, my, 12, TOPBAR_Y, 170, 16)) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;

        // Light/dark toggle
        int tx = 12 + 170 + 10;
        if (GuiRender.inside(mx, my, tx, TOPBAR_Y, 44, 16)) {
            Themes.setDark(!Themes.isDark());
            return true;
        }

        // Accent swatches
        int ax = tx + 54;
        for (int i = 0; i < Themes.ACCENT_SWATCHES.length; i++) {
            int x = ax + i * (SWATCH + 4);
            if (GuiRender.inside(mx, my, x, TOPBAR_Y + 1, SWATCH, SWATCH)) {
                Themes.setAccent(Themes.ACCENT_SWATCHES[i]);
                return true;
            }
        }

        // Panels
        for (Map.Entry<ModuleCategory, int[]> e : panelPos.entrySet()) {
            int px = e.getValue()[0];
            int py = e.getValue()[1];
            if (GuiRender.inside(mx, my, px, py, PANEL_W, HEADER_H)) {
                dragging = e.getKey();
                dragOffX = (int) mx - px;
                dragOffY = (int) my - py;
                return true;
            }
            List<AxoModule> mods = modulesIn(e.getKey());
            int ry = py + HEADER_H;
            for (AxoModule m : mods) {
                if (GuiRender.inside(mx, my, px, ry, PANEL_W, ROW_H)) {
                    if (button == 1) {
                        awaitingBind = m;
                    } else {
                        ModuleManager.get().toggle(m);
                    }
                    return true;
                }
                ry += ROW_H;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging != null) {
            double mx = event.x();
            double my = event.y();
            int[] pos = panelPos.get(dragging);
            pos[0] = Math.max(0, Math.min(this.width - PANEL_W, (int) mx - dragOffX));
            pos[1] = Math.max(28, Math.min(this.height - 24, (int) my - dragOffY));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            int[] pos = panelPos.get(dragging);
            ModuleManager.get().config().setModuleInt(key(dragging), "x", pos[0]);
            ModuleManager.get().config().setModuleInt(key(dragging), "y", pos[1]);
            dragging = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (awaitingBind != null) {
            Keybinds.setKey(awaitingBind, keyCode == GLFW.GLFW_KEY_ESCAPE ? -1 : keyCode);
            awaitingBind = null;
            return true;
        }
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!search.isEmpty()) {
                    search = search.substring(0, search.length() - 1);
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchFocused && event.codepoint() >= ' ') {
            search += event.codepointAsString();
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
