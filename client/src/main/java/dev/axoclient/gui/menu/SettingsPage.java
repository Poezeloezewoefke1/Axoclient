package dev.axoclient.gui.menu;

import dev.axoclient.gui.HudEditorScreen;
import dev.axoclient.gui.theme.Palette;
import dev.axoclient.input.Keybinds;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/** The Settings page: how the menu itself looks and behaves. */
final class SettingsPage implements MenuPage {
    private static final int ROW_H = 32;

    private record Switch(String label, String desc, Supplier<Boolean> get, Consumer<Boolean> set) {}

    private static final List<Switch> SWITCHES = List.of(
        new Switch("Background blur", "Blur the game world behind the menu",
            MenuSettings::blur, MenuSettings::setBlur),
        new Switch("Menu animations", "Slide the menu in when it opens",
            MenuSettings::animations, MenuSettings::setAnimations),
        new Switch("Module notifications", "Show a toast when a module is switched on or off",
            MenuSettings::toggleNotifications, MenuSettings::setToggleNotifications),
        new Switch("Show keybinds on cards", "Display each module's key under its name",
            MenuSettings::keybindsOnCards, MenuSettings::setKeybindsOnCards),
        new Switch("Pause singleplayer", "Pause your world while the menu is open",
            MenuSettings::pauseGame, MenuSettings::setPauseGame)
    );

    private int scroll;
    private int maxScroll;
    private boolean listening;

    @Override
    public String navLabel() {
        return "Settings";
    }

    @Override
    public String navCount() {
        return "";
    }

    @Override
    public String titleAccent() {
        return "General";
    }

    @Override
    public String titleRest() {
        return "Settings";
    }

    @Override
    public String subtitle() {
        return "Menu behaviour and appearance";
    }

    @Override
    public void shown() {
        listening = false;
    }

    @Override
    public void render(GuiGraphics g, Palette p, Hits hits, int x, int y, int w, int h, int mx, int my, String query) {
        int rows = 1 + SWITCHES.size() + 2;
        maxScroll = Math.max(0, rows * (ROW_H + 5) - 5 - h);
        scroll = Math.min(scroll, maxScroll);
        g.enableScissor(x, y, x + w, y + h);
        hits.clip(x, y, w, h);
        int ry = y - scroll;

        // Menu size.
        row(g, p, x, ry, w, "Menu size", "How much of the screen the menu takes");
        List<String> sizes = List.of("Small", "Normal", "Large");
        int segW = Draw.segmentedWidth(sizes, 7);
        int[] edges = Draw.segmented(g, x + w - 10 - segW, ry + 8, 16, sizes, MenuSettings.size(), p, 7);
        for (int i = 0; i < sizes.size(); i++) {
            int index = i;
            hits.add(edges[i], ry + 8, edges[i + 1] - edges[i], 16, () -> MenuSettings.setSize(index));
        }
        ry += ROW_H + 5;

        for (Switch s : SWITCHES) {
            row(g, p, x, ry, w, s.label(), s.desc());
            boolean on = s.get().get();
            Draw.toggle(g, x + w - 34, ry + 10, 24, 13, on, p);
            hits.add(x + w - 40, ry, 40, ROW_H, () -> s.set().accept(!s.get().get()));
            ry += ROW_H + 5;
        }

        // Menu keybind.
        row(g, p, x, ry, w, "Menu keybind", "Key that opens and closes this menu");
        String key = listening ? "PRESS A KEY" : Keybinds.keyName(MenuSettings.menuKey()).toUpperCase(Locale.ROOT);
        int kw = Math.max(56, Draw.width(key) + 16);
        Draw.card(g, x + w - 10 - kw, ry + 8, kw, 16, 4, p.raised(), listening ? p.accent() : p.border());
        Draw.centered(g, key, x + w - 10 - kw / 2, ry + 12, listening ? p.accent() : p.text());
        hits.add(x + w - 10 - kw, ry + 8, kw, 16, () -> listening = true);
        ry += ROW_H + 5;

        // HUD layout editor.
        row(g, p, x, ry, w, "HUD layout", "Drag your HUD elements to where you want them");
        String open = "Open editor";
        int ow = Draw.width(open) + 16;
        Draw.card(g, x + w - 10 - ow, ry + 8, ow, 16, 4, p.raised(), p.border());
        Draw.centered(g, open, x + w - 10 - ow / 2, ry + 12, p.text());
        hits.add(x + w - 10 - ow, ry + 8, ow, 16, () -> Minecraft.getInstance().setScreen(new HudEditorScreen()));

        hits.unclip();
        g.disableScissor();
    }

    private static void row(GuiGraphics g, Palette p, int x, int y, int w, String label, String desc) {
        Draw.card(g, x, y, w, ROW_H, 6, p.card(), p.border());
        Draw.text(g, label, x + 10, y + 7, p.text());
        Draw.text(g, Draw.trim(desc, w - 150), x + 10, y + 18, p.sub());
    }

    @Override
    public boolean capturingKeys() {
        return listening;
    }

    @Override
    public boolean key(int glfwKey) {
        if (listening) {
            if (glfwKey != GLFW.GLFW_KEY_ESCAPE) {
                MenuSettings.setMenuKey(glfwKey);
                Keybinds.warnConflicts(glfwKey, "menu");
            }
            listening = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean scroll(double mx, double my, double amount) {
        scroll = Math.max(0, Math.min(maxScroll, scroll + (int) Math.round(-amount * 24)));
        return true;
    }
}
