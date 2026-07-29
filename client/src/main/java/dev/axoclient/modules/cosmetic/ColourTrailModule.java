package dev.axoclient.modules.cosmetic;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.core.ModuleSetting;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * A trail in a colour you choose, rather than one of the twenty fixed
 * particle types.
 *
 * Config (module "colour_trail"):
 *   colour        — 0xRRGGBB. -1 (the default) follows your ClickGUI accent,
 *                   so the trail matches your theme without any setup.
 *   scale         — particle size, in tenths (10 = 1.0)
 *   density_ticks — lower means more particles
 *
 * Local-only, like every Axo cosmetic.
 */
public final class ColourTrailModule extends AxoModule {
    private static final double SPREAD = 0.4;

    public ColourTrailModule() {
        super("colour_trail", "Colour Trail", ModuleCategory.COSMETIC, false);
    }

    /**
     * Colour is not listed here: it follows your ClickGUI accent by default,
     * and the accent swatches are right there in the same screen. A raw
     * 0xRRGGBB override is still available in the config file.
     */
    @Override
    public List<ModuleSetting> settings() {
        return List.of(
            new ModuleSetting(id(), "scale", "Size", 1, 40, 1, 10, ModuleSetting.Format.TENTHS),
            ModuleSetting.plain(id(), "density_ticks", "Every N ticks", 1, 20, 1)
        );
    }

    /** Read per tick, not cached at enable, so ClickGUI edits apply instantly. */
    private int densityTicks() {
        return Math.max(1, ModuleManager.get().config().getModuleInt(id(), "density_ticks", 1));
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || player.tickCount % densityTicks() != 0) {
            return;
        }

        double ox = (Math.random() - 0.5) * SPREAD;
        double oz = (Math.random() - 0.5) * SPREAD;
        minecraft.level.addParticle(
            Trails.themedDust(id(), scale()),
            player.getX() + ox,
            player.getY() + 0.1,
            player.getZ() + oz,
            0.0,
            0.0,
            0.0
        );
    }

    private float scale() {
        int tenths = ModuleManager.get().config().getModuleInt(id(), "scale", 10);
        return Math.max(1, Math.min(40, tenths)) / 10.0f;
    }
}
