package dev.axoclient.modules.qol;

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
 * Press a key to copy your current coordinates to the clipboard.
 *
 * <h2>Why the key is a setting and the feedback is in chat</h2>
 * The original hard-coded F8 with a toast as its only feedback, and it was
 * reported as "does nothing". Both halves of that are now addressed: the key
 * is rebindable from the mod menu, so a key that is swallowed by an overlay
 * or the OS can be changed; and the confirmation goes to chat as well as the
 * toast, so there is a durable, unmissable record that the press registered.
 * If chat shows the line, the clipboard was written.
 */
public final class CopyCoordsModule extends AxoModule {
    private static final int DEFAULT_KEY = GLFW.GLFW_KEY_F8;

    private boolean wasDown;

    public CopyCoordsModule() {
        super("copy_coords", "Copy Coords", ModuleCategory.QOL, true);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(
            ModuleSetting.key(id(), "copy_key", "Copy key", DEFAULT_KEY),
            ModuleSetting.choice(
                id(), "style", "Format", List.of("123 64 -87", "123, 64, -87", "x:123 y:64 z:-87"), 0
            ),
            ModuleSetting.choice(id(), "feedback", "Confirm in", List.of("Chat + toast", "Toast only"), 0)
        );
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
            if (ModuleManager.get().config().getModuleInt(id(), "feedback", 0) == 0) {
                player.displayClientMessage(Component.literal("§b[Axo] §fCopied to clipboard: §a" + coords), false);
            }
        }
        wasDown = down;
    }

    /** Whole blocks — nobody pastes fractional coordinates into chat. */
    private String format(double x, double y, double z) {
        long bx = Math.round(x);
        long by = Math.round(y);
        long bz = Math.round(z);
        return switch (ModuleManager.get().config().getModuleInt(id(), "style", 0)) {
            case 1 -> bx + ", " + by + ", " + bz;
            case 2 -> "x:" + bx + " y:" + by + " z:" + bz;
            default -> bx + " " + by + " " + bz;
        };
    }
}
