package dev.axoclient.modules.cosmetic;

import dev.axoclient.core.ModuleManager;
import dev.axoclient.gui.theme.Themes;
import net.minecraft.core.particles.DustParticleOptions;

/**
 * Shared helpers for the coloured cosmetics.
 *
 * They all follow the same rule: colour comes from your ClickGUI accent
 * unless a module overrides it, so picking an accent swatch recolours your
 * whole look in one click instead of module by module.
 */
public final class Trails {
    /** Config value meaning "use the theme accent". */
    public static final int FOLLOW_ACCENT = -1;

    private Trails() {
    }

    /** Configured 0xRRGGBB for this module, or the theme accent. */
    public static int themedColour(String moduleId) {
        int configured = ModuleManager.get().config().getModuleInt(moduleId, "colour", FOLLOW_ACCENT);
        if (configured != FOLLOW_ACCENT) {
            return configured & 0xFFFFFF;
        }
        return Themes.current().accent & 0xFFFFFF;
    }

    public static DustParticleOptions themedDust(String moduleId, float scale) {
        return new DustParticleOptions(themedColour(moduleId), scale);
    }
}
