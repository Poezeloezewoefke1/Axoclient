package dev.axoclient.modules.pvp;

import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleSetting;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Counts down from your last hit given or taken, so you know when you're
 * clear of a server's combat tag before logging out or teleporting. Most PvP
 * servers tag for 10–30 seconds; set the length to match yours.
 *
 * Hits given are seen the same way as the combo counter (the target's health
 * dropping); hits taken from your own health dropping.
 *
 * Config (module "combat_timer"):
 *   seconds — tag length
 */
public final class CombatTimerModule extends HudModule {
    private static final float MIN_DAMAGE = 0.05F;

    private long taggedUntil;
    private float lastOwnHealth = -1;
    private int trackedId = -1;
    private float lastTargetHealth;

    public CombatTimerModule() {
        super("combat_timer", "Combat Timer", ModuleCategory.PVP, new HudPosition(HudAnchor.TOP_CENTER, 0, 104), false);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(new ModuleSetting(id(), "seconds", "Tag length (s)", 5, 60, 5, 15, ModuleSetting.Format.PLAIN));
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        taggedUntil = 0;
        lastOwnHealth = -1;
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            lastOwnHealth = -1;
            return;
        }
        boolean hit = false;
        float health = player.getHealth();
        if (lastOwnHealth >= 0 && lastOwnHealth - health >= MIN_DAMAGE && health > 0) {
            hit = true;
        }
        lastOwnHealth = health;

        Entity target = minecraft.crosshairPickEntity;
        if (target instanceof LivingEntity living) {
            float targetHealth = living.getHealth();
            if (living.getId() == trackedId && lastTargetHealth - targetHealth >= MIN_DAMAGE) {
                hit = true;
            }
            trackedId = living.getId();
            lastTargetHealth = targetHealth;
        }
        if (hit) {
            taggedUntil = System.currentTimeMillis() + settings().get(0).get() * 1000L;
        }
    }

    @Override
    protected String hudText() {
        long left = taggedUntil - System.currentTimeMillis();
        if (left <= 0 || Minecraft.getInstance().player == null) {
            return null;
        }
        return "In combat · " + (left + 999) / 1000 + "s";
    }
}
