package dev.axoclient.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.util.Keys;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;

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
