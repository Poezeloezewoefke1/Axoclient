package dev.axoclient.gui.menu;

import dev.axoclient.core.AxoConfig;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.gui.theme.Themes;
import org.lwjgl.glfw.GLFW;

/**
 * The menu's own preferences from its Settings page, stored alongside the
 * theme in the "gui" config section.
 */
public final class MenuSettings {
    public static final int DEFAULT_MENU_KEY = GLFW.GLFW_KEY_RIGHT_SHIFT;

    private MenuSettings() {
    }

    private static AxoConfig config() {
        return ModuleManager.get().config();
    }

    private static boolean flag(String key, boolean fallback) {
        return config().getModuleBool(Themes.SECTION, key, fallback);
    }

    private static void setFlag(String key, boolean value) {
        config().setModuleBool(Themes.SECTION, key, value);
    }

    /** Key that opens and closes the menu. */
    public static int menuKey() {
        int key = config().getModuleInt(Themes.SECTION, "menu_key", DEFAULT_MENU_KEY);
        return key > 0 ? key : DEFAULT_MENU_KEY;
    }

    public static void setMenuKey(int glfwKey) {
        config().setModuleInt(Themes.SECTION, "menu_key", glfwKey);
    }

    /** 0 small, 1 normal, 2 large: how much of the screen the menu window takes. */
    public static int size() {
        return Math.max(0, Math.min(2, config().getModuleInt(Themes.SECTION, "menu_size", 1)));
    }

    public static void setSize(int size) {
        config().setModuleInt(Themes.SECTION, "menu_size", size);
    }

    public static boolean listView() {
        return flag("list_view", false);
    }

    public static void setListView(boolean list) {
        setFlag("list_view", list);
    }

    public static boolean blur() {
        return flag("blur", true);
    }

    public static void setBlur(boolean on) {
        setFlag("blur", on);
    }

    public static boolean animations() {
        return flag("animations", true);
    }

    public static void setAnimations(boolean on) {
        setFlag("animations", on);
    }

    /** A toast when a module is switched on or off. */
    public static boolean toggleNotifications() {
        return flag("toggle_notifications", true);
    }

    public static void setToggleNotifications(boolean on) {
        setFlag("toggle_notifications", on);
    }

    public static boolean keybindsOnCards() {
        return flag("keybinds_on_cards", true);
    }

    public static void setKeybindsOnCards(boolean on) {
        setFlag("keybinds_on_cards", on);
    }

    /** Pause a singleplayer world while the menu is open. */
    public static boolean pauseGame() {
        return flag("pause_game", false);
    }

    public static void setPauseGame(boolean on) {
        setFlag("pause_game", on);
    }
}
