package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Remembers where you last died so you can walk back to your stuff. The
 * position is captured the tick your health hits zero, then kept on screen
 * until the next death.
 */
public final class DeathCoordsModule extends HudModule {
    private String lastDeath;
    private boolean wasDead;

    public DeathCoordsModule() {
        super("death_coords", "Death Coords", new HudPosition(HudAnchor.BOTTOM_LEFT, 4, -14), false);
    }

    @Override
    public void onTick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        boolean dead = player.getHealth() <= 0.0F;
        if (dead && !wasDead) {
            lastDeath = String.format(
                Locale.ROOT,
                "Died at %d, %d, %d",
                (int) Math.floor(player.getX()),
                (int) Math.floor(player.getY()),
                (int) Math.floor(player.getZ())
            );
        }
        wasDead = dead;
    }

    @Override
    protected String hudText() {
        return Minecraft.getInstance().player == null ? null : lastDeath;
    }
}
