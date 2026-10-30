package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * While you're falling far enough to get hurt, how far you've fallen and
 * roughly what it will cost: one half-heart per block past the first three.
 * Armour, Feather Falling and soft landings reduce that, so it is the worst
 * case — the point is to notice in time to place water.
 */
public final class FallWarningModule extends HudModule {
    private static final double SAFE_BLOCKS = 3.0;

    public FallWarningModule() {
        super("fall_warning", "Fall Warning", new HudPosition(HudAnchor.CENTER, 0, 34), false);
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.onGround() || player.isFallFlying() || player.getAbilities().flying
            || player.isInWater()) {
            return null;
        }
        // fallDistance became a double in 1.21.5; the cast reads either type.
        double fallen = (double) player.fallDistance;
        if (fallen <= SAFE_BLOCKS) {
            return null;
        }
        int damage = (int) Math.ceil(fallen - SAFE_BLOCKS);
        return String.format(Locale.ROOT, "Falling %.0f blocks · up to %.1f hearts", fallen, damage / 2.0);
    }
}
