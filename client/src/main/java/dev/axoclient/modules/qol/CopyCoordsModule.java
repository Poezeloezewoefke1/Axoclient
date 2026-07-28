package dev.axoclient.modules.qol;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.gui.notify.Notifications;
import dev.axoclient.util.Keys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;

/**
 * Press a key to copy your current coordinates to the clipboard, ready to
 * paste into chat or Discord.
 *
 * This is an "action" module rather than a toggle, so it polls its own key
 * instead of registering with {@link dev.axoclient.input.Keybinds} (which
 * toggles modules on and off). The key is config-backed under "copy_key".
 */
public final class CopyCoordsModule extends AxoModule {
    private static final int DEFAULT_KEY = GLFW.GLFW_KEY_F8;

    private boolean wasDown;

    public CopyCoordsModule() {
        super("copy_coords", "Copy Coords", ModuleCategory.QOL, true);
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null) {
            wasDown = false;
            return;
        }
        int key = ModuleManager.get().config().getModuleInt(id(), "copy_key", DEFAULT_KEY);
        boolean down = key > 0 && Keys.isDown(key);
        if (down && !wasDown) {
            String coords = format(player.getX(), player.getY(), player.getZ());
            minecraft.keyboardHandler.setClipboard(coords);
            Notifications.success("Copied " + coords);
        }
        wasDown = down;
    }

    /** Whole blocks — nobody pastes fractional coordinates into chat. */
    private static String format(double x, double y, double z) {
        return Math.round(x) + " " + Math.round(y) + " " + Math.round(z);
    }
}
