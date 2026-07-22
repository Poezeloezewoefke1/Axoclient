package dev.axoclient.modules.cosmetic;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.cosmetic.Capes;
import net.minecraft.resources.ResourceLocation;

/**
 * A custom cape you (and only you) can see — a bundled texture applied to your
 * own player via the skin mixin. Last one enabled wins. Off by default.
 */
public final class CapeModule extends AxoModule {
    private final ResourceLocation texture;

    public CapeModule(String id, String displayName, String texturePath) {
        super(id, displayName, ModuleCategory.COSMETIC, false);
        this.texture = ResourceLocation.fromNamespaceAndPath("axoclient", texturePath);
    }

    @Override
    protected void onEnable() {
        Capes.set(texture);
    }

    @Override
    protected void onDisable() {
        Capes.clear(texture);
    }
}
