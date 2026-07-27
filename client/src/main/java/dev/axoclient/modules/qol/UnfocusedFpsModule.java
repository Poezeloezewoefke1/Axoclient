package dev.axoclient.modules.qol;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import net.minecraft.client.Minecraft;

/**
 * Drops the frame limit while the game window isn't focused, so alt-tabbing
 * stops cooking your GPU and draining laptop battery. The original limit is
 * restored the moment you click back in — or when the module is turned off.
 */
public final class UnfocusedFpsModule extends AxoModule {
    private static final int BACKGROUND_FPS = 10;

    private Integer previousLimit;

    public UnfocusedFpsModule() {
        super("unfocused_fps", "Background FPS Limit", ModuleCategory.QOL, false);
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean focused = minecraft.isWindowActive();
        if (!focused && previousLimit == null) {
            previousLimit = minecraft.options.framerateLimit().get();
            minecraft.options.framerateLimit().set(BACKGROUND_FPS);
        } else if (focused && previousLimit != null) {
            minecraft.options.framerateLimit().set(previousLimit);
            previousLimit = null;
        }
    }

    @Override
    protected void onDisable() {
        restore();
    }

    private void restore() {
        if (previousLimit != null) {
            Minecraft.getInstance().options.framerateLimit().set(previousLimit);
            previousLimit = null;
        }
    }
}
