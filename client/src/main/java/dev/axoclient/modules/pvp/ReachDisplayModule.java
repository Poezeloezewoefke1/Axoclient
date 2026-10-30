package dev.axoclient.modules.pvp;

import dev.axoclient.core.ModuleCategory;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.EntityHitResult;

/**
 * How far away your last swing connected from, eye to the point on the
 * target's hitbox the crosshair was on. Reads the hit the client already
 * picked for the crosshair, so it only shows what the vanilla client knows.
 */
public final class ReachDisplayModule extends HudModule {
    /** Ticks the last reading stays up after a swing (20 = 1s). */
    private static final int HOLD_TICKS = 60;

    private boolean attackWasDown;
    private double lastReach;
    private int holdLeft;

    public ReachDisplayModule() {
        super("reach_display", "Reach Display", ModuleCategory.PVP, new HudPosition(HudAnchor.CENTER, 0, 20), false);
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        boolean down = minecraft.options.keyAttack.isDown();
        if (down && !attackWasDown && minecraft.hitResult instanceof EntityHitResult hit) {
            lastReach = player.getEyePosition().distanceTo(hit.getLocation());
            holdLeft = HOLD_TICKS;
        } else if (holdLeft > 0) {
            holdLeft--;
        }
        attackWasDown = down;
    }

    @Override
    protected String hudText() {
        if (holdLeft <= 0 || Minecraft.getInstance().player == null) {
            return null;
        }
        return String.format(Locale.ROOT, "%.2f blocks", lastReach);
    }
}
