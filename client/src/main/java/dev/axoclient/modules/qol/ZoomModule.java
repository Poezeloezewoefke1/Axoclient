package dev.axoclient.modules.qol;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.util.Keys;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Hold-to-zoom (roadmap P1-11): holding C drops the FOV to 30 and
 * releasing restores the exact previous value. Smooth interpolation and
 * scroll-to-adjust come later; rebinding comes with P1-05.
 */
public final class ZoomModule extends AxoModule {
    private static final int ZOOM_KEY = GLFW.GLFW_KEY_C;
    private static final int ZOOM_FOV = 30;

    private Integer previousFov;

    public ZoomModule() {
        super("zoom", "Zoom", ModuleCategory.QOL, true);
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean held = minecraft.screen == null && Keys.isDown(ZOOM_KEY);
        if (held && previousFov == null) {
            previousFov = minecraft.options.fov().get();
            minecraft.options.fov().set(ZOOM_FOV);
        } else if (!held && previousFov != null) {
            minecraft.options.fov().set(previousFov);
            previousFov = null;
        }
    }

    @Override
    protected void onDisable() {
        if (previousFov != null) {
            Minecraft.getInstance().options.fov().set(previousFov);
            previousFov = null;
        }
    }
}
