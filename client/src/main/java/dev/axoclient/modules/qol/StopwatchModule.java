package dev.axoclient.modules.qol;

import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.core.ModuleSetting;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import dev.axoclient.util.Keys;
import dev.axoclient.util.TimeFormat;
import java.util.List;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * A stopwatch on the HUD for parkour runs, farm cycles or speedrun splits.
 * One key starts and pauses, another resets. Runs on real time, so it keeps
 * counting through lag spikes exactly like a stopwatch in your hand would.
 */
public final class StopwatchModule extends HudModule {
    private static final int DEFAULT_START_KEY = GLFW.GLFW_KEY_K;

    private long accumulated;
    private long runningSince = -1;
    private boolean startWasDown;
    private boolean resetWasDown;

    public StopwatchModule() {
        super("stopwatch", "Stopwatch", ModuleCategory.QOL, new HudPosition(HudAnchor.TOP_CENTER, 0, 76), false);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(
            ModuleSetting.key(id(), "start_key", "Start / pause key", DEFAULT_START_KEY),
            ModuleSetting.key(id(), "reset_key", "Reset key", 0)
        );
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        accumulated = 0;
        runningSince = -1;
    }

    @Override
    public void onTick() {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        boolean startDown = isDown(ModuleManager.get().config().getModuleInt(id(), "start_key", DEFAULT_START_KEY));
        boolean resetDown = isDown(ModuleManager.get().config().getModuleInt(id(), "reset_key", 0));
        long now = System.currentTimeMillis();
        if (startDown && !startWasDown) {
            if (runningSince < 0) {
                runningSince = now;
            } else {
                accumulated += now - runningSince;
                runningSince = -1;
            }
        }
        if (resetDown && !resetWasDown) {
            accumulated = 0;
            runningSince = runningSince < 0 ? -1 : now;
        }
        startWasDown = startDown;
        resetWasDown = resetDown;
    }

    @Override
    protected String hudText() {
        long elapsed = accumulated + (runningSince < 0 ? 0 : System.currentTimeMillis() - runningSince);
        String state = runningSince < 0 && elapsed > 0 ? "  (paused)" : "";
        return "Timer " + TimeFormat.stopwatch(elapsed) + state;
    }

    private static boolean isDown(int key) {
        return key > 0 && Keys.isDown(key);
    }
}
