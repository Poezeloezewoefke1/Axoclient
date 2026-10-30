package dev.axoclient.gui.menu;

import dev.axoclient.gui.HudEditorScreen;
import dev.axoclient.gui.menu.Ui.Type;
import dev.axoclient.gui.theme.Palette;
import dev.axoclient.input.Keybinds;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/** The Settings page: how the menu itself looks and behaves. Rows are the design's 66px cards. */
final class SettingsPage implements MenuPage {
    private static final int ROW_H = 66;

    private record Switch(String label, String desc, Supplier<Boolean> get, Consumer<Boolean> set) {}

    private static final List<Switch> SWITCHES = List.of(
        new Switch("Background blur", "Blur the game world behind the menu",
            MenuSettings::blur, MenuSettings::setBlur),
        new Switch("Menu animations", "Smooth transitions when opening the menu",
            MenuSettings::animations, MenuSettings::setAnimations),
        new Switch("Module notifications", "Show a toast when a module is toggled",
            MenuSettings::toggleNotifications, MenuSettings::setToggleNotifications),
        new Switch("Show keybinds on cards", "Display each module key under its name",
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
        int contentH = rows * (ROW_H + 8) - 8 + 24;
        maxScroll = Math.max(0, contentH - h);
        scroll = Math.min(scroll, maxScroll);
        int rowW = w - 4;
        int right = x + rowW - 18;
        g.enableScissor(x, y, x + w, y + h);
        hits.clip(x, y, w, h);
        int ry = y - scroll;

        // GUI scale → Menu size: Small / Normal / Large, buttons 30px, padding 0 14.
        row(g, p, x, ry, rowW, "Menu size", "Size of the client menu on your screen");
        List<String> sizes = List.of("Small", "Normal", "Large");
        int[] edges = Ui.segmented(g, p, right, ry + ROW_H / 2 - 18, 36, sizes, MenuSettings.size(), Type.S12, 14, 7);
        for (int i = 0; i < sizes.size(); i++) {
            int index = i;
            hits.add(edges[i], ry + ROW_H / 2 - 18, edges[i + 1] - edges[i], 36, () -> MenuSettings.setSize(index));
        }
        ry += ROW_H + 8;

        for (Switch s : SWITCHES) {
            row(g, p, x, ry, rowW, s.label(), s.desc());
            Ui.toggle(g, right - 38, ry + ROW_H / 2 - 11, 38, 22, s.get().get(), p);
            hits.add(right - 44, ry, 50, ROW_H, () -> s.set().accept(!s.get().get()));
            ry += ROW_H + 8;
        }

        row(g, p, x, ry, rowW, "Menu keybind", "Key that opens and closes this menu");
        String key = listening ? "PRESS A KEY" : Keybinds.keyName(MenuSettings.menuKey()).toUpperCase(Locale.ROOT);
        keyButton(g, p, right, ry + ROW_H / 2, key, listening);
        int kw = Ui.spacedWidth(Type.B11, key, 0.14f) + 28;
        hits.add(right - kw, ry + ROW_H / 2 - 16, kw, 32, () -> listening = true);
        ry += ROW_H + 8;

        row(g, p, x, ry, rowW, "HUD layout", "Drag your HUD elements to where you want them");
        keyButton(g, p, right, ry + ROW_H / 2, "OPEN EDITOR", false);
        int ow = Ui.spacedWidth(Type.B11, "OPEN EDITOR", 0.14f) + 28;
        hits.add(right - ow, ry + ROW_H / 2 - 16, ow, 32, () -> Minecraft.getInstance().setScreen(new HudEditorScreen()));

        hits.unclip();
        g.disableScissor();
        ModulesPage.scrollbar(g, x + w, y, h, contentH, scroll);
    }

    private static void row(GuiGraphics g, Palette p, int x, int y, int w, String label, String desc) {
        Ui.box(g, x, y, w, ROW_H, 12, p.card(), p.border());
        int mid = y + ROW_H / 2;
        Ui.text(g, Type.S14, label, x + 19, mid - 17, p.text());
        Ui.text(g, Type.R11, desc, x + 19, mid + 3, p.sub());
    }

    /** The design's keybind button: 32px, padding 0 14, radius 8, raised, 11px bold letter-spaced. */
    private static void keyButton(GuiGraphics g, Palette p, int right, int mid, String label, boolean active) {
        int w = Ui.spacedWidth(Type.B11, label, 0.14f) + 28;
        Ui.box(g, right - w, mid - 16, w, 32, 8, p.raised(), active ? p.accent() : p.border());
        Ui.spacedCentered(g, Type.B11, label, right - w / 2, mid, active ? p.accent() : p.text(), 0.14f);
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
        scroll = Math.max(0, Math.min(maxScroll, scroll + (int) Math.round(-amount * 40)));
        return true;
    }
}
