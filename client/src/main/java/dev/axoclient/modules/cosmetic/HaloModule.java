package dev.axoclient.modules.cosmetic;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.core.ModuleSetting;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.Level;

/**
 * A ring of light that turns slowly above your head.
 *
 * This is a particle halo, not a 3D model hat. A real modelled accessory
 * needs a player-renderer mixin plus custom geometry, which is both a lot of
 * surface area to break on a Minecraft update and something that looks wrong
 * unless the model is properly made. An orbiting ring gets a clean, readable
 * effect out of the same no-mixin particle path the other cosmetics use.
 *
 * Local-only, like every Axo cosmetic.
 */
public final class HaloModule extends AxoModule {
    private static final double DEFAULT_RADIUS_HUNDREDTHS = 40;
    private static final double HEIGHT = 2.15;
    /** Degrees the ring turns per tick. */
    private static final double SPIN_PER_TICK = 6.0;

    public HaloModule() {
        super("halo", "Halo", ModuleCategory.COSMETIC, false);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(
            new ModuleSetting(
                id(), "radius", "Radius", 10, 120, 5,
                (int) DEFAULT_RADIUS_HUNDREDTHS, ModuleSetting.Format.TENTHS
            ),
            ModuleSetting.plain(id(), "points", "Points", 3, 24, 8),
            ModuleSetting.plain(id(), "density_ticks", "Every N ticks", 1, 20, 2)
        );
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        int every = Math.max(1, ModuleManager.get().config().getModuleInt(id(), "density_ticks", 2));
        if (player.tickCount % every != 0) {
            return;
        }

        // radius is stored in tenths to match the setting's display format
        double radius = ModuleManager.get().config().getModuleInt(id(), "radius", 40) / 100.0;
        int points = Math.max(3, ModuleManager.get().config().getModuleInt(id(), "points", 8));
        double spin = Math.toRadians(player.tickCount * SPIN_PER_TICK);
        DustParticleOptions dust = Trails.themedDust(id(), 1.0f);

        for (int i = 0; i < points; i++) {
            double angle = spin + (Math.PI * 2 * i) / points;
            level.addParticle(
                dust,
                player.getX() + Math.cos(angle) * radius,
                player.getY() + HEIGHT,
                player.getZ() + Math.sin(angle) * radius,
                0.0,
                0.0,
                0.0
            );
        }
    }
}
