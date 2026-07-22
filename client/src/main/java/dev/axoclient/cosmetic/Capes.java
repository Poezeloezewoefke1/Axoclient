package dev.axoclient.cosmetic;

import net.minecraft.resources.ResourceLocation;

/**
 * Holds the currently active custom cape texture (client-side, local-only).
 * Cape modules set/clear it; {@link dev.axoclient.mixin.PlayerCapeMixin} reads
 * it when supplying the local player's skin. Null means no custom cape.
 */
public final class Capes {
    private static ResourceLocation active;

    private Capes() {
    }

    public static void set(ResourceLocation texture) {
        active = texture;
    }

    public static void clear(ResourceLocation texture) {
        if (texture != null && texture.equals(active)) {
            active = null;
        }
    }

    public static ResourceLocation active() {
        return active;
    }
}
