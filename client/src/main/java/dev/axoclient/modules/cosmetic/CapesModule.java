package dev.axoclient.modules.cosmetic;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.core.ModuleSetting;
import dev.axoclient.cosmetic.Capes;
import java.util.List;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;

/**
 * One cape toggle, with the colour chosen in its settings.
 *
 * This replaces ten separate on/off modules. Ten toggles for what is really
 * one decision meant the menu carried ten rows that were mutually exclusive
 * in spirit but not in the UI — nothing stopped you enabling three, and "last
 * one enabled wins" was the only thing resolving it.
 *
 * Local-only: only you see it. Off by default.
 */
public final class CapesModule extends AxoModule {
    /** Display name -> texture path under assets/axoclient/. */
    private static final List<String> NAMES = List.of(
        "Blue", "Red", "Purple", "Black", "Green", "White", "Gold", "Sunset", "Ocean", "Carbon"
    );
    private static final List<String> PATHS = List.of(
        "textures/capes/blue.png",
        "textures/capes/red.png",
        "textures/capes/purple.png",
        "textures/capes/black.png",
        "textures/capes/green.png",
        "textures/capes/white.png",
        "textures/capes/gold.png",
        "textures/capes/sunset.png",
        "textures/capes/ocean.png",
        "textures/capes/carbon.png"
    );

    /** The cape currently pushed to {@link Capes}, so we can clear exactly it. */
    private ClientAsset.ResourceTexture applied;
    private int appliedIndex = -1;

    public CapesModule() {
        super("capes", "Capes", ModuleCategory.COSMETIC, false);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(ModuleSetting.choice(id(), "style", "Cape", NAMES, 0));
    }

    @Override
    protected void onEnable() {
        apply(styleIndex());
    }

    @Override
    protected void onDisable() {
        clear();
    }

    /**
     * Watch for the setting changing while enabled, so picking a new colour in
     * the menu swaps the cape immediately instead of after a re-toggle.
     */
    @Override
    public void onTick() {
        int wanted = styleIndex();
        if (wanted != appliedIndex) {
            apply(wanted);
        }
    }

    private void apply(int index) {
        clear();
        Identifier location = Identifier.fromNamespaceAndPath("axoclient", PATHS.get(index));
        applied = new ClientAsset.ResourceTexture(location, location);
        appliedIndex = index;
        Capes.set(applied);
    }

    private void clear() {
        if (applied != null) {
            Capes.clear(applied);
            applied = null;
        }
        appliedIndex = -1;
    }

    private int styleIndex() {
        int stored = ModuleManager.get().config().getModuleInt(id(), "style", 0);
        return Math.max(0, Math.min(PATHS.size() - 1, stored));
    }
}
