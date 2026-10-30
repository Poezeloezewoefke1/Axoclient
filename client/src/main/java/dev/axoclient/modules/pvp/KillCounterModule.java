package dev.axoclient.modules.pvp;

import dev.axoclient.core.ModuleCategory;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Kills this session, players counted separately. A kill is something you
 * hit that dies within two seconds of your last hit on it, which is what the
 * client can see without the server's combat log. Shared kills count for
 * whoever hit last from your point of view, so it's a close estimate rather
 * than an official tally.
 */
public final class KillCounterModule extends HudModule {
    private static final int CREDIT_TICKS = 40;
    private static final float MIN_DAMAGE = 0.05F;

    private LivingEntity lastHit;
    private int ticksSinceHit;
    private int trackedId = -1;
    private float lastHealth;
    private int kills;
    private int playerKills;

    public KillCounterModule() {
        super("kill_counter", "Kill Counter", ModuleCategory.PVP, new HudPosition(HudAnchor.TOP_RIGHT, -4, 124), false);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        kills = 0;
        playerKills = 0;
        lastHit = null;
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            lastHit = null;
            return;
        }
        Entity target = minecraft.crosshairPickEntity;
        if (target instanceof LivingEntity living) {
            float health = living.getHealth();
            if (living.getId() == trackedId && lastHealth - health >= MIN_DAMAGE) {
                lastHit = living;
                ticksSinceHit = 0;
            }
            trackedId = living.getId();
            lastHealth = health;
        }
        if (lastHit == null) {
            return;
        }
        if (lastHit.isDeadOrDying()) {
            kills++;
            if (lastHit instanceof Player) {
                playerKills++;
            }
            lastHit = null;
        } else if (++ticksSinceHit > CREDIT_TICKS) {
            lastHit = null;
        }
    }

    @Override
    protected String hudText() {
        if (Minecraft.getInstance().player == null) {
            return null;
        }
        return "Kills " + kills + (playerKills > 0 ? " · " + playerKills + " players" : "");
    }
}
