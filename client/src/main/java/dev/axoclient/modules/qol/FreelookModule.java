package dev.axoclient.modules.qol;

import dev.axoclient.camera.Freelook;
import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.gui.notify.Notifications;
import dev.axoclient.util.Keys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;

/**
 * Hold a key to look around while your body keeps facing — and running — the
 * way it was. Useful for watching behind you mid-chase.
 *
 * Like Copy Coords this is a hold-key action rather than a toggle, so it
 * polls its own key. The bind lives under "look_key".
 */
public final class FreelookModule extends AxoModule {
    private static final int DEFAULT_KEY = GLFW.GLFW_KEY_LEFT_ALT;

    private boolean held;
    private boolean warned;

    public FreelookModule() {
        super("freelook", "Freelook", ModuleCategory.QOL, false);
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null) {
            release();
            return;
        }

        int key = ModuleManager.get().config().getModuleInt(id(), "look_key", DEFAULT_KEY);
        boolean down = key > 0 && Keys.isDown(key);

        if (down && !held) {
            Freelook.start(player.getYRot(), player.getXRot());
            // engaged() stays false when the camera hook did not apply, which
            // is the one case worth telling the player about — otherwise the
            // key would just seem dead.
            if (!Freelook.engaged() && !warned) {
                Notifications.info("Freelook is not available on this version");
                warned = true;
            }
        } else if (!down && held) {
            Freelook.stop();
        }
        held = down;
    }

    @Override
    protected void onDisable() {
        release();
    }

    private void release() {
        if (held) {
            Freelook.stop();
            held = false;
        }
    }
}
