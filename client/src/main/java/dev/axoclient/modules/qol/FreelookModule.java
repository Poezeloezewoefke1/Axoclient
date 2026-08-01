package dev.axoclient.modules.qol;

import dev.axoclient.camera.Freelook;
import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.core.ModuleSetting;
import dev.axoclient.gui.notify.Notifications;
import dev.axoclient.util.Keys;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Look around while your body keeps facing — and running — the way it was.
 *
 * <h2>Diagnosing "it does nothing"</h2>
 * Freelook depends on two optional mixins, and an optional mixin is allowed
 * to fail silently. That made the two possible causes of a dead key —
 * "the key never reached us" and "the camera hook isn't applied" —
 * indistinguishable to the player.
 *
 * So the key is now a rebindable setting, and the first press reports which
 * of the two it is: engaging normally, or a hard warning that the camera hook
 * is missing on this version. Whatever the log or the player reports next,
 * it now distinguishes the two cases instead of just "nothing happened".
 */
public final class FreelookModule extends AxoModule {
    private static final int DEFAULT_KEY = GLFW.GLFW_KEY_LEFT_ALT;

    private boolean held;
    /** Toggle mode only: whether freelook is currently latched on. */
    private boolean latched;
    private boolean warned;

    public FreelookModule() {
        super("freelook", "Freelook", ModuleCategory.QOL, false);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(
            ModuleSetting.key(id(), "look_key", "Freelook key", DEFAULT_KEY),
            ModuleSetting.choice(id(), "mode", "Activation", List.of("Hold", "Toggle"), 0)
        );
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
        boolean toggleMode = ModuleManager.get().config().getModuleInt(id(), "mode", 0) == 1;

        boolean pressed = down && !held;
        held = down;

        boolean wantActive;
        if (toggleMode) {
            if (pressed) {
                latched = !latched;
            }
            wantActive = latched;
        } else {
            wantActive = down;
        }

        if (wantActive) {
            if (!Freelook.engaged()) {
                Freelook.start(player.getYRot(), player.getXRot());
            }
            // engaged() stays false when the camera hook did not apply. Say so
            // once, loudly — a silent dead key is what made this hard to report.
            if (!Freelook.engaged() && pressed && !warned) {
                Notifications.info("Freelook unavailable: camera hook missing");
                player.displayClientMessage(
                    Component.literal(
                        "§b[Axo] §fFreelook could not start — the camera hook is not applied on this "
                            + "Minecraft version. Please report this with your latest game log."
                    ),
                    false
                );
                warned = true;
            }
        } else {
            Freelook.stop();
        }
    }

    @Override
    protected void onDisable() {
        release();
    }

    private void release() {
        latched = false;
        held = false;
        Freelook.stop();
    }
}
