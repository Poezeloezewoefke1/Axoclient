package dev.axoclient.modules.cosmetic;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;

/** Floating hearts above your player — a local-only cosmetic. Off by default. */
public final class HeartTrailModule extends AxoModule {

    public HeartTrailModule() {
        super("heart_trail", "Heart Trail", ModuleCategory.COSMETIC, false);
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        if (player.tickCount % 4 == 0) {
            minecraft.level.addParticle(
                ParticleTypes.HEART,
                player.getX(),
                player.getY() + 1.0,
                player.getZ(),
                0.0,
                0.0,
                0.0
            );
        }
    }
}
