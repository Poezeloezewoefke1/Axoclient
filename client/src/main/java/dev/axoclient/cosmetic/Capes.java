package dev.axoclient.cosmetic;

import net.minecraft.core.ClientAsset;

/**
 * Holds the active custom cape texture (client-side, local-only). Cape modules
 * set/clear it; {@link dev.axoclient.mixin.PlayerCapeMixin} patches it into the
 * local player's skin. Null means no custom cape. Stored as a
 * {@link ClientAsset.ResourceTexture} because that is what a
 * {@code PlayerSkin.Patch} accepts for the cape slot in 1.21.11.
 */
public final class Capes {
    private static ClientAsset.ResourceTexture active;

    private Capes() {
    }

    public static void set(ClientAsset.ResourceTexture texture) {
        active = texture;
    }

    public static void clear(ClientAsset.ResourceTexture texture) {
        if (texture != null && texture.equals(active)) {
            active = null;
        }
    }

    public static ClientAsset.ResourceTexture active() {
        return active;
    }
}
