package dev.axoclient.modules.hud;

import dev.axoclient.core.ModuleSetting;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

/**
 * How many hostile mobs are close by, counted from entities the client can
 * already see. Matches on Enemy rather than Monster so slimes, phantoms and
 * the like count too. Recounted twice a second, not every frame.
 *
 * Config (module "hostile_counter"):
 *   radius — how far to look, in blocks
 */
public final class HostileCounterModule extends HudModule {
    private static final int DEFAULT_RADIUS = 16;
    private static final int RECOUNT_TICKS = 10;

    private int count;
    private int ticks;

    public HostileCounterModule() {
        super("hostile_counter", "Hostile Counter", new HudPosition(HudAnchor.TOP_RIGHT, -4, 112), false);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(ModuleSetting.plain(id(), "radius", "Radius", 4, 64, DEFAULT_RADIUS));
    }

    @Override
    public void onTick() {
        if (++ticks < RECOUNT_TICKS) {
            return;
        }
        ticks = 0;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            count = 0;
            return;
        }
        int radius = settings().get(0).get();
        count = minecraft.level.getEntitiesOfClass(
            LivingEntity.class,
            player.getBoundingBox().inflate(radius),
            entity -> entity instanceof Enemy && entity.isAlive()
        ).size();
    }

    @Override
    protected String hudText() {
        if (Minecraft.getInstance().player == null) {
            return null;
        }
        return count == 0 ? "No hostiles nearby" : count + (count == 1 ? " hostile nearby" : " hostiles nearby");
    }
}
