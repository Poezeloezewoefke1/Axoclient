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
 * Shows how much health your target just lost. Derived by watching the
 * target's health between ticks rather than hooking the damage pipeline —
 * no mixin, and it reads only values the client already receives.
 */
public final class DamageNumbersModule extends HudModule {
    private static final int SHOW_TICKS = 30;
    /** Ignore rounding noise from health syncs. */
    private static final float MIN_DAMAGE = 0.05F;

    private int trackedId = -1;
    private float lastHealth;
    private float lastDamage;
    private int showTicksLeft;

    public DamageNumbersModule() {
        super("damage_numbers", "Damage Numbers", ModuleCategory.PVP, new HudPosition(HudAnchor.CENTER, 20, -10), false);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        trackedId = -1;
        showTicksLeft = 0;
    }

    @Override
    public void onTick() {
        if (showTicksLeft > 0) {
            showTicksLeft--;
        }
        Entity target = Minecraft.getInstance().crosshairPickEntity;
        if (!(target instanceof LivingEntity living)) {
            return;
        }
        float health = living.getHealth();
        if (living.getId() != trackedId) {
            trackedId = living.getId();
            lastHealth = health;
            return;
        }
        float drop = lastHealth - health;
        if (drop >= MIN_DAMAGE) {
            lastDamage = drop;
            showTicksLeft = SHOW_TICKS;
        }
        lastHealth = health;
    }

    @Override
    protected String hudText() {
        if (showTicksLeft <= 0 || Minecraft.getInstance().player == null) {
            return null;
        }
        return String.format(Locale.ROOT, "-%.1f", lastDamage);
    }
}
