package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;

/** JVM memory usage on the HUD — quick heap-pressure read without F3. */
public final class MemoryHudModule extends HudModule {

    public MemoryHudModule() {
        super("memory_hud", "Memory", new HudPosition(HudAnchor.TOP_RIGHT, -4, 16), false);
    }

    @Override
    protected String hudText() {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory() / 1048576L;
        long used = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
        int percent = max > 0 ? (int) (used * 100L / max) : 0;
        return "Mem " + used + "/" + max + "MB (" + percent + "%)";
    }
}
