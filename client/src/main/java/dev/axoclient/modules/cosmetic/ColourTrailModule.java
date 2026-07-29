package dev.axoclient.modules.cosmetic;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.gui.theme.Themes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;

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
    private static final int FOLLOW_ACCENT = -1;
    private static final double SPREAD = 0.4;

    private int everyTicks = 1;

    public ColourTrailModule() {
        super("colour_trail", "Colour Trail", ModuleCategory.COSMETIC, false);
    }

    @Override
    protected void onEnable() {
        everyTicks = Math.max(1, ModuleManager.get().config().getModuleInt(id(), "density_ticks", 1));
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || player.tickCount % everyTicks != 0) {
            return;
        }

        double ox = (Math.random() - 0.5) * SPREAD;
        double oz = (Math.random() - 0.5) * SPREAD;
        minecraft.level.addParticle(
            new DustParticleOptions(rgb(), scale()),
            player.getX() + ox,
            player.getY() + 0.1,
            player.getZ() + oz,
            0.0,
            0.0,
            0.0
        );
    }

    /** Configured colour, or the current theme accent with its alpha stripped. */
    private int rgb() {
        int configured = ModuleManager.get().config().getModuleInt(id(), "colour", FOLLOW_ACCENT);
        if (configured != FOLLOW_ACCENT) {
            return configured & 0xFFFFFF;
        }
        return Themes.current().accent & 0xFFFFFF;
    }

    private float scale() {
        int tenths = ModuleManager.get().config().getModuleInt(id(), "scale", 10);
        return Math.max(1, Math.min(40, tenths)) / 10.0f;
    }
}
