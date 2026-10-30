package dev.axoclient.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.core.ModuleSetting;
import dev.axoclient.gui.notify.Notification;
import dev.axoclient.gui.notify.Notifications;
import dev.axoclient.util.Keys;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Config-backed module toggle keys (rebindable from the ClickGUI). A per-tick
 * poll edge-detects each bound key and toggles its module. Keys are ignored
 * while a screen is open so typing in the GUI never fires a bind.
 */
public final class Keybinds {
    private static final Map<String, Boolean> PREV_DOWN = new HashMap<>();

    private Keybinds() {
    }

    /** GLFW key bound to a module, or -1 when unbound. */
    public static int keyOf(AxoModule module) {
        return ModuleManager.get().config().getModuleInt(module.id(), "key", module.defaultToggleKey());
    }

    public static void setKey(AxoModule module, int glfwKey) {
        ModuleManager.get().config().setModuleInt(module.id(), "key", glfwKey);
    }

    /** Human-readable key name (e.g. "C", "Right Shift"), or "" when unbound. */
    public static String keyName(int glfwKey) {
        if (glfwKey <= 0) {
            return "";
        }
        try {
            return InputConstants.Type.KEYSYM.getOrCreate(glfwKey).getDisplayName().getString();
        } catch (RuntimeException e) {
            return "?";
        }
    }

    /**
     * Everything else that uses {@code glfwKey}: vanilla controls by their
     * on-screen name, module toggle keys, and module key settings. The binding
     * being set is excluded by {@code selfId} — "toggle:&lt;module id&gt;" for a
     * toggle key, "&lt;module id&gt;.&lt;setting key&gt;" for a key setting.
     */
    public static List<String> conflicts(int glfwKey, String selfId) {
        List<String> out = new ArrayList<>();
        if (glfwKey <= 0) {
            return out;
        }
        String keyName = InputConstants.Type.KEYSYM.getOrCreate(glfwKey).getName();
        for (KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
            if (mapping.saveString().equals(keyName)) {
                out.add(Component.translatable(mapping.getName()).getString());
            }
        }
        if (glfwKey == dev.axoclient.gui.menu.MenuSettings.menuKey() && !selfId.equals("menu")) {
            out.add("Open Axo menu");
        }
        for (AxoModule module : ModuleManager.get().all()) {
            if (keyOf(module) == glfwKey && !selfId.equals("toggle:" + module.id())) {
                out.add(module.displayName() + " (toggle)");
            }
            for (ModuleSetting setting : module.settings()) {
                if (setting.format() == ModuleSetting.Format.KEY
                    && setting.get() == glfwKey
                    && !selfId.equals(module.id() + "." + setting.key())) {
                    out.add(module.displayName() + " · " + setting.label());
                }
            }
        }
        return out;
    }

    /** Pushes a warning naming every conflict for a key the player just bound. Silent when there are none. */
    public static void warnConflicts(int glfwKey, String selfId) {
        List<String> clashes = conflicts(glfwKey, selfId);
        if (!clashes.isEmpty()) {
            Notifications.push(
                keyName(glfwKey) + " is also used by: " + String.join(", ", clashes),
                Notification.Type.WARN
            );
        }
    }

    /** Called each client tick: toggle modules whose bound key was just pressed. */
    public static void tick() {
        if (Minecraft.getInstance().screen != null) {
            return; // don't fire binds while a GUI/screen is open
        }
        for (AxoModule module : ModuleManager.get().all()) {
            int key = keyOf(module);
            if (key <= 0) {
                continue;
            }
            boolean down = Keys.isDown(key);
            boolean prev = PREV_DOWN.getOrDefault(module.id(), false);
            if (down && !prev) {
                ModuleManager.get().toggle(module);
            }
            PREV_DOWN.put(module.id(), down);
        }
    }
}
