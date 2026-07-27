package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Estimated server tick rate. The world clock only advances when the server
 * ticks, so comparing how far it moved against real elapsed time tells you
 * whether the server is keeping up — the difference between "I'm lagging"
 * and "the server is lagging".
 */
public final class TpsHudModule extends HudModule {
    /** Sample window; long enough to be steady, short enough to react. */
    private static final long SAMPLE_MS = 2000;

    private long lastSampleAt;
    private long lastGameTime = -1;
    private double tps = -1;

    public TpsHudModule() {
        super("tps_hud", "Server TPS", new HudPosition(HudAnchor.TOP_RIGHT, -4, 52), false);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        lastGameTime = -1;
        tps = -1;
    }

    @Override
    public void onTick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            lastGameTime = -1;
            tps = -1;
            return;
        }
        long now = System.currentTimeMillis();
        long gameTime = level.getGameTime();
        if (lastGameTime < 0) {
            lastGameTime = gameTime;
            lastSampleAt = now;
            return;
        }
        long elapsed = now - lastSampleAt;
        if (elapsed >= SAMPLE_MS) {
            double measured = (gameTime - lastGameTime) * 1000.0 / elapsed;
            // A server never exceeds 20 TPS; anything above is measurement noise.
            tps = Math.min(20.0, measured);
            lastGameTime = gameTime;
            lastSampleAt = now;
        }
    }

    @Override
    protected String hudText() {
        if (Minecraft.getInstance().level == null || tps < 0) {
            return null;
        }
        return String.format(Locale.ROOT, "%.1f TPS", tps);
    }
}
