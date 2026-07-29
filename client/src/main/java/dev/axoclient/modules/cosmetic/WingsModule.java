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
 * A pair of swept wings traced behind your shoulders, in your accent colour.
 *
 * Particles rather than a model, for the same reason as {@link HaloModule}:
 * modelled accessories need a player-renderer mixin and hand-built geometry.
 * Two mirrored arcs that follow your facing read convincingly as wings and
 * cost nothing in port risk.
 *
 * The arcs are rebuilt every emission from the player's current yaw, so they
 * turn with you instead of lagging behind.
 */
public final class WingsModule extends AxoModule {
    /** Points per wing. More is denser, not bigger. */
    private static final int DEFAULT_POINTS = 7;
    /** How far behind the body the wings sit. */
    private static final double BACK_OFFSET = 0.18;
    private static final double SHOULDER_HEIGHT = 1.05;

    public WingsModule() {
        super("wings", "Wings", ModuleCategory.COSMETIC, false);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(
            new ModuleSetting(id(), "span", "Span", 4, 24, 1, 9, ModuleSetting.Format.TENTHS),
            ModuleSetting.plain(id(), "points", "Points per wing", 3, 16, DEFAULT_POINTS),
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

        double span = ModuleManager.get().config().getModuleInt(id(), "span", 9) / 10.0;
        int points = Math.max(3, ModuleManager.get().config().getModuleInt(id(), "points", DEFAULT_POINTS));
        DustParticleOptions dust = Trails.themedDust(id(), 0.9f);

        // Yaw 0 faces +Z in Minecraft, so "right" is (cos, sin) and "forward"
        // is (-sin, cos). Wings hang off the right vector, pushed backwards.
        double yaw = Math.toRadians(player.getYRot());
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);
        double backX = Math.sin(yaw);
        double backZ = -Math.cos(yaw);

        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < points; i++) {
                double t = i / (double) (points - 1);
                // Sweep out and up: the tip ends higher than the shoulder.
                double lateral = (0.18 + t * span) * side;
                double height = SHOULDER_HEIGHT + t * t * 0.75;
                double back = BACK_OFFSET + t * 0.25;

                level.addParticle(
                    dust,
                    player.getX() + rightX * lateral + backX * back,
                    player.getY() + height,
                    player.getZ() + rightZ * lateral + backZ * back,
                    0.0,
                    0.0,
                    0.0
                );
            }
        }
    }
}
