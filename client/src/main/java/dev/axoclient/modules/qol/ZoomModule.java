package dev.axoclient.modules.qol;

import dev.axoclient.camera.Zoom;
import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.core.ModuleSetting;
import dev.axoclient.util.Keys;
import java.util.List;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Hold a key to zoom. The easing lives in {@link Zoom} and is applied per
 * frame by {@code FovMixin}, so the ramp is smooth instead of stepping once
 * per tick through whole-degree FOV values.
 *
 * The fallback path below is what runs if that mixin ever stops applying: it
 * writes {@code options.fov()} on the tick, like the original implementation.
 * Visibly steppy, but zoom still works.
 *
 * Config (module "zoom"):
 *   fov  — zoomed field of view
 *   ease — ramp length in milliseconds
 *   key  — hold-to-zoom key
 */
public final class ZoomModule extends AxoModule {
    private static final int DEFAULT_KEY = GLFW.GLFW_KEY_C;
    private static final int DEFAULT_ZOOM_FOV = 30;
    private static final int DEFAULT_EASE_MS = 180;

    /** Fallback path only: FOV before we touched it, to be restored exactly. */
    private Integer baseFov;
    private double fallbackProgress;

    public ZoomModule() {
        super("zoom", "Zoom", ModuleCategory.QOL, true);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(
            ModuleSetting.plain(id(), "fov", "Zoomed FOV", 1, 110, DEFAULT_ZOOM_FOV),
            new ModuleSetting(id(), "ease", "Ease time", 0, 600, 20, DEFAULT_EASE_MS, ModuleSetting.Format.PLAIN),
            ModuleSetting.key(id(), "zoom_key", "Zoom key", DEFAULT_KEY)
        );
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        int key = ModuleManager.get().config().getModuleInt(id(), "zoom_key", DEFAULT_KEY);
        boolean held = key > 0 && minecraft.screen == null && Keys.isDown(key);

        Zoom.configure(zoomFov(), easeMillis());
        Zoom.setActive(held);

        if (Zoom.hookAlive()) {
            // The renderer hook owns the FOV now. If we had previously taken
            // the fallback path, hand the player's own setting back.
            restoreFallback();
            return;
        }
        tickFallback(minecraft, held);
    }

    @Override
    protected void onDisable() {
        Zoom.reset();
        restoreFallback();
    }

    /** Tick-driven, integer-stepped zoom. Only used when FovMixin is absent. */
    private void tickFallback(Minecraft minecraft, boolean held) {
        if (held && baseFov == null) {
            baseFov = minecraft.options.fov().get();
        }
        if (baseFov == null) {
            return;
        }
        // Ease time is in milliseconds; a client tick is 50ms.
        double step = 1.0 / Math.max(1.0, easeMillis() / 50.0);
        fallbackProgress = held
            ? Math.min(1.0, fallbackProgress + step)
            : Math.max(0.0, fallbackProgress - step);

        if (fallbackProgress <= 0.0) {
            restoreFallback();
            return;
        }
        int from = baseFov;
        minecraft.options.fov().set((int) Math.round(from + (zoomFov() - from) * fallbackProgress));
    }

    private void restoreFallback() {
        if (baseFov != null) {
            Minecraft.getInstance().options.fov().set(baseFov);
            baseFov = null;
        }
        fallbackProgress = 0.0;
    }

    private int zoomFov() {
        return clamp(ModuleManager.get().config().getModuleInt(id(), "fov", DEFAULT_ZOOM_FOV), 1, 110);
    }

    private int easeMillis() {
        return clamp(ModuleManager.get().config().getModuleInt(id(), "ease", DEFAULT_EASE_MS), 1, 600);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
