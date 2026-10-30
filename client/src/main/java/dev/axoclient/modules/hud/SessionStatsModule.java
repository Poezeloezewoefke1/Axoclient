package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import dev.axoclient.waypoint.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * What this play session has added up to: ground covered and deaths.
 * Counts from game start (or from enabling the module), across worlds.
 */
public final class SessionStatsModule extends HudModule {
    /** A jump bigger than this in one tick is a teleport or respawn, not travel. */
    private static final double TELEPORT_BLOCKS = 10.0;

    private double travelled;
    private int deaths;
    private boolean wasDead;
    private boolean hasLast;
    private double lastX;
    private double lastZ;

    public SessionStatsModule() {
        super("session_stats", "Session Stats", new HudPosition(HudAnchor.TOP_LEFT, 4, 148), false);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        travelled = 0;
        deaths = 0;
        hasLast = false;
    }

    @Override
    public void onTick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            hasLast = false;
            return;
        }
        if (hasLast) {
            double dx = player.getX() - lastX;
            double dz = player.getZ() - lastZ;
            double step = Math.sqrt(dx * dx + dz * dz);
            if (step < TELEPORT_BLOCKS) {
                travelled += step;
            }
        }
        lastX = player.getX();
        lastZ = player.getZ();
        hasLast = true;

        boolean dead = player.getHealth() <= 0.0F;
        if (dead && !wasDead) {
            deaths++;
        }
        wasDead = dead;
    }

    @Override
    protected String hudText() {
        if (Minecraft.getInstance().player == null) {
            return null;
        }
        return "Travelled " + Waypoint.formatDistance(travelled) + " · " + deaths + (deaths == 1 ? " death" : " deaths");
    }
}
