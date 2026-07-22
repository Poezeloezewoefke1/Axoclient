package dev.axoclient.modules.cosmetic;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;

/**
 * A configurable client-side particle cosmetic: emits a chosen particle at or
 * above the local player every N ticks with a little horizontal spread. One
 * concrete class drives every particle trail/aura, so adding a new cosmetic is
 * just one more registration in {@link dev.axoclient.AxoClient}. Local-only —
 * only you see it. Off by default.
 */
public final class ParticleCosmeticModule extends AxoModule {
    private final ParticleOptions particle;
    private final int everyTicks;
    private final double yOffset;
    private final double spread;

    public ParticleCosmeticModule(
        String id,
        String displayName,
        ParticleOptions particle,
        int everyTicks,
        double yOffset
    ) {
        super(id, displayName, ModuleCategory.COSMETIC, false);
        this.particle = particle;
        this.everyTicks = Math.max(1, everyTicks);
        this.yOffset = yOffset;
        this.spread = 0.4;
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        if (player.tickCount % everyTicks != 0) {
            return;
        }
        double ox = (Math.random() - 0.5) * spread;
        double oz = (Math.random() - 0.5) * spread;
        minecraft.level.addParticle(
            particle,
            player.getX() + ox,
            player.getY() + yOffset,
            player.getZ() + oz,
            0.0,
            0.0,
            0.0
        );
    }
}
