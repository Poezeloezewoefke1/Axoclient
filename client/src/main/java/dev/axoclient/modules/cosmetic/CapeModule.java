package dev.axoclient.modules.cosmetic;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.cosmetic.Capes;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;

/**
 * A custom cape only you can see — a bundled texture patched onto your own
 * player's skin. Last one enabled wins. Off by default.
 */
public final class CapeModule extends AxoModule {
    private final ClientAsset.ResourceTexture cape;

    public CapeModule(String id, String displayName, String texturePath) {
        super(id, displayName, ModuleCategory.COSMETIC, false);
        Identifier location = Identifier.fromNamespaceAndPath("axoclient", texturePath);
        this.cape = new ClientAsset.ResourceTexture(location, location);
    }

    @Override
    protected void onEnable() {
        Capes.set(cape);
    }

    @Override
    protected void onDisable() {
        Capes.clear(cape);
    }
}
