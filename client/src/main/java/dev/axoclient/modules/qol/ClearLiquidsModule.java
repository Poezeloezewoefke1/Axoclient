package dev.axoclient.modules.qol;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.core.ModuleSetting;
import java.util.List;

/**
 * Removes the full-screen water and lava overlays, so swimming and lava dips
 * do not blind you.
 *
 * The state lives here and is read by {@code ScreenEffectMixin} each frame;
 * the module itself does no work per tick. Water and lava are separate
 * switches because they are wanted for different reasons — water for
 * visibility while swimming, lava mainly for Nether travel.
 */
public final class ClearLiquidsModule extends AxoModule {

    public ClearLiquidsModule() {
        super("clear_liquids", "Clear Water & Lava", ModuleCategory.QOL, false);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(
            ModuleSetting.choice(id(), "water", "Water overlay", List.of("Hidden", "Vanilla"), 0),
            ModuleSetting.choice(id(), "lava", "Lava overlay", List.of("Hidden", "Vanilla"), 0),
            ModuleSetting.choice(id(), "powder_snow", "Powder snow overlay", List.of("Hidden", "Vanilla"), 0)
        );
    }

    /** True when the module is on and the given overlay is set to hidden. */
    private static boolean hides(String key) {
        ModuleManager modules = ModuleManager.get();
        return modules.byId("clear_liquids")
            .filter(modules::isEnabled)
            .map(m -> modules.config().getModuleInt(m.id(), key, 0) == 0)
            .orElse(false);
    }

    public static boolean hidesWater() {
        return hides("water");
    }

    public static boolean hidesLava() {
        return hides("lava");
    }

    public static boolean hidesPowderSnow() {
        return hides("powder_snow");
    }

    /** True when every overlay this module can hide is currently hidden. */
    public static boolean hidesEverything() {
        return hidesWater() && hidesLava() && hidesPowderSnow();
    }
}
