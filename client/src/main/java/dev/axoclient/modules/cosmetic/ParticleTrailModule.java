package dev.axoclient.modules.cosmetic;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;

/**
 * A client-side particle trail that follows your player — a purely local
 * cosmetic (only you see it; making cosmetics visible to others needs a
 * backend, which this project deliberately avoids). Off by default.
 */
public final class ParticleTrailModule extends AxoModule {

    public ParticleTrailModule() {
        super("particle_trail", "Particle Trail", ModuleCategory.COSMETIC, false);
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        minecraft.level.addParticle(
            ParticleTypes.END_ROD,
            player.getX(),
            player.getY() + 0.1,
            player.getZ(),
            0.0,
            0.0,
            0.0
        );
    }
}
