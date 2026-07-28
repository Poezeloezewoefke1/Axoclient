package dev.axoclient.modules.qol;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.util.Keys;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Hold-to-zoom: holding C eases the FOV down, releasing eases it back to the
 * exact value you started on.
 *
 * The easing runs on the client tick rather than per frame, so it is FPS
 * independent — at 20 ticks a second the default 4-tick ramp is about 200ms,
 * which reads as instant without the jarring snap.
 *
 * Config (module "zoom"):
 *   fov    — zoomed field of view
 *   smooth — ticks the ramp takes; 1 disables easing (snap zoom)
 */
public final class ZoomModule extends AxoModule {
    private static final int ZOOM_KEY = GLFW.GLFW_KEY_C;
    private static final int DEFAULT_ZOOM_FOV = 30;
    private static final int DEFAULT_SMOOTH_TICKS = 4;

    /** FOV before we touched it — the value we must restore exactly. */
    private Integer baseFov;
    /** 0 = fully out, 1 = fully zoomed. */
    private double progress;

    public ZoomModule() {
        super("zoom", "Zoom", ModuleCategory.QOL, true);
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean held = minecraft.screen == null && Keys.isDown(ZOOM_KEY);

        if (held && baseFov == null) {
            baseFov = minecraft.options.fov().get();
        }
        if (baseFov == null) {
            return;
        }

        double step = 1.0 / smoothTicks();
        progress = held ? Math.min(1.0, progress + step) : Math.max(0.0, progress - step);

        if (progress <= 0.0) {
            // Fully back out: restore the original value and let go of it, so a
            // change made in the options menu isn't clobbered by the next zoom.
            minecraft.options.fov().set(baseFov);
            baseFov = null;
            return;
        }
        minecraft.options.fov().set(lerpFov(baseFov, zoomFov(), ease(progress)));
    }

    @Override
    protected void onDisable() {
        if (baseFov != null) {
            Minecraft.getInstance().options.fov().set(baseFov);
            baseFov = null;
        }
        progress = 0.0;
    }

    private int zoomFov() {
        return clamp(ModuleManager.get().config().getModuleInt(id(), "fov", DEFAULT_ZOOM_FOV), 1, 110);
    }

    private int smoothTicks() {
        return clamp(ModuleManager.get().config().getModuleInt(id(), "smooth", DEFAULT_SMOOTH_TICKS), 1, 20);
    }

    private static int lerpFov(int from, int to, double t) {
        return (int) Math.round(from + (to - from) * t);
    }

    /** Ease-out cubic: quick off the mark, settles gently. */
    private static double ease(double t) {
        double inverted = 1.0 - t;
        return 1.0 - inverted * inverted * inverted;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
