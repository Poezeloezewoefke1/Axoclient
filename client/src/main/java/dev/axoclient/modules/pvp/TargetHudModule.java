package dev.axoclient.modules.pvp;

import dev.axoclient.core.ModuleCategory;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Health of whatever you're aiming at. Reads the entity the client already
 * picked for the crosshair, so it shows exactly what you'd hit — and only
 * information the vanilla client already has.
 */
public final class TargetHudModule extends HudModule {
    /** Keep the last target on screen briefly so it doesn't flicker mid-fight. */
    private static final int HOLD_TICKS = 40;

    private String lastTarget;
    private int holdLeft;

    public TargetHudModule() {
        super("target_hud", "Target Info", ModuleCategory.PVP, new HudPosition(HudAnchor.TOP_CENTER, 0, 48), false);
    }

    @Override
    public void onTick() {
        String current = describeTarget();
        if (current != null) {
            lastTarget = current;
            holdLeft = HOLD_TICKS;
        } else if (holdLeft > 0) {
            holdLeft--;
        }
    }

    @Override
    protected String hudText() {
        return holdLeft > 0 ? lastTarget : null;
    }

    private static String describeTarget() {
        Minecraft minecraft = Minecraft.getInstance();
        Entity target = minecraft.crosshairPickEntity;
        if (minecraft.player == null || !(target instanceof LivingEntity living)) {
            return null;
        }
        return String.format(
            Locale.ROOT,
            "%s  %.0f/%.0f HP  %.1fm",
            living.getName().getString(),
            living.getHealth(),
            living.getMaxHealth(),
            minecraft.player.distanceTo(living)
        );
    }
}
