package dev.axoclient.modules.qol;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.mixin.OptionInstanceAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

/**
 * Fullbright (roadmap P1-10): raises gamma far past the vanilla cap via
 * the raw OptionInstance accessor, and restores the exact previous value
 * on disable. Off by default — it visibly changes the game.
 */
public final class FullbrightModule extends AxoModule {
    private static final double BOOSTED_GAMMA = 15.0;

    private Double previousGamma;

    public FullbrightModule() {
        super("fullbright", "Fullbright", ModuleCategory.QOL, false);
    }

    @Override
    protected void onEnable() {
        OptionInstance<Double> gamma = Minecraft.getInstance().options.gamma();
        previousGamma = gamma.get();
        setRaw(gamma, BOOSTED_GAMMA);
    }

    @Override
    protected void onDisable() {
        if (previousGamma != null) {
            // Normal set(): the previous value is legal, let the validator see it.
            Minecraft.getInstance().options.gamma().set(previousGamma);
            previousGamma = null;
        }
    }

    @SuppressWarnings("unchecked")
    private static void setRaw(OptionInstance<Double> option, double value) {
        ((OptionInstanceAccessor<Double>) (Object) option).axoclient$setRawValue(value);
    }
}
