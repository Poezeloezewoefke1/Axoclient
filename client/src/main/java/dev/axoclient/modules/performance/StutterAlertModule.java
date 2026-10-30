package dev.axoclient.modules.performance;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.HudRenderable;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleSetting;
import dev.axoclient.gui.notify.Notification;
import dev.axoclient.gui.notify.Notifications;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Tells you when a single frame froze for longer than you'd like, which an
 * FPS counter averages away. Helps tell a stutter from network lag.
 *
 * Measured between HUD frames. Gaps longer than a few seconds are ignored,
 * because those are the window being minimised or a world loading rather
 * than a stutter, and alerts are spaced out so a rough patch doesn't spam.
 *
 * Config (module "stutter_alert"):
 *   ms — frame time that counts as a stutter
 */
public final class StutterAlertModule extends AxoModule implements HudRenderable {
    private static final long IGNORE_ABOVE_MS = 5000;
    private static final long COOLDOWN_MS = 5000;

    private long lastFrame;
    private long lastAlert;

    public StutterAlertModule() {
        super("stutter_alert", "Stutter Alert", ModuleCategory.PERFORMANCE, false);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(new ModuleSetting(id(), "ms", "Alert above (ms)", 100, 2000, 50, 250, ModuleSetting.Format.PLAIN));
    }

    @Override
    protected void onEnable() {
        lastFrame = 0;
    }

    @Override
    public void renderHud(GuiGraphics graphics) {
        long now = System.currentTimeMillis();
        long gap = lastFrame == 0 ? 0 : now - lastFrame;
        lastFrame = now;
        if (!Minecraft.getInstance().isWindowActive() || gap > IGNORE_ABOVE_MS || now - lastAlert < COOLDOWN_MS) {
            return;
        }
        if (gap >= settings().get(0).get()) {
            lastAlert = now;
            Notifications.push("Stutter: one frame took " + gap + " ms", Notification.Type.WARN);
        }
    }
}
