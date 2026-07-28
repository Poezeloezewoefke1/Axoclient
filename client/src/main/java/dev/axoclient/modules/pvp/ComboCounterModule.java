package dev.axoclient.modules.pvp;

import dev.axoclient.core.ModuleCategory;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Counts hits landed in a row on the same target. Like the damage readout,
 * this watches the target's health rather than hooking combat, so it needs
 * no mixin. The streak ends when you stop connecting for a moment or you
 * switch targets.
 */
public final class ComboCounterModule extends HudModule {
    /** Ticks without a hit before the streak is considered over (20 = 1s). */
    private static final int RESET_TICKS = 40;
    private static final float MIN_DAMAGE = 0.05F;

    private int trackedId = -1;
    private float lastHealth;
    private int combo;
    private int ticksSinceHit;

    public ComboCounterModule() {
        super("combo_counter", "Combo Counter", ModuleCategory.PVP, new HudPosition(HudAnchor.CENTER, -60, -10), false);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        trackedId = -1;
        combo = 0;
    }

    @Override
    public void onTick() {
        if (combo > 0 && ++ticksSinceHit > RESET_TICKS) {
            combo = 0;
        }
        Entity target = Minecraft.getInstance().crosshairPickEntity;
        if (!(target instanceof LivingEntity living)) {
            return;
        }
        float health = living.getHealth();
        if (living.getId() != trackedId) {
            // New target: a streak is per-opponent.
            trackedId = living.getId();
            lastHealth = health;
            combo = 0;
            return;
        }
        if (lastHealth - health >= MIN_DAMAGE) {
            combo++;
            ticksSinceHit = 0;
        }
        lastHealth = health;
    }

    @Override
    protected String hudText() {
        if (combo < 2 || Minecraft.getInstance().player == null) {
            return null;
        }
        return combo + " hit combo";
    }
}
