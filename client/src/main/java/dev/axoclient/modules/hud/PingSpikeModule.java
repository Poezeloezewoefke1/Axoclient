package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

/**
 * Warns when latency suddenly jumps well above its recent average — the
 * moment your hits stop registering. Stays silent on a steady connection so
 * it isn't just a second ping counter.
 */
public final class PingSpikeModule extends HudModule {
    /** Ignore jitter below this; only real spikes are worth interrupting for. */
    private static final int MIN_SPIKE_MS = 200;
    private static final double SPIKE_FACTOR = 2.0;
    /** Ticks the warning stays up after a spike (20 ticks = 1 second). */
    private static final int HOLD_TICKS = 60;

    private double average = -1;
    private int warnTicksLeft;
    private int spikeMs;

    public PingSpikeModule() {
        super("ping_spike", "Ping Spike Warning", new HudPosition(HudAnchor.TOP_CENTER, 0, 20), false);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        average = -1;
        warnTicksLeft = 0;
    }

    @Override
    public void onTick() {
        Integer ping = currentPing();
        if (ping == null) {
            average = -1;
            warnTicksLeft = 0;
            return;
        }
        if (average < 0) {
            average = ping;
            return;
        }
        if (ping >= MIN_SPIKE_MS && ping > average * SPIKE_FACTOR) {
            spikeMs = ping;
            warnTicksLeft = HOLD_TICKS;
        } else if (warnTicksLeft > 0) {
            warnTicksLeft--;
        }
        // Slow-moving average so one spike doesn't hide the next.
        average = average * 0.9 + ping * 0.1;
    }

    @Override
    protected String hudText() {
        return warnTicksLeft > 0 ? "! Lag spike " + spikeMs + " ms" : null;
    }

    private static Integer currentPing() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (minecraft.player == null || connection == null) {
            return null;
        }
        PlayerInfo info = connection.getPlayerInfo(minecraft.player.getUUID());
        return info == null ? null : info.getLatency();
    }
}
