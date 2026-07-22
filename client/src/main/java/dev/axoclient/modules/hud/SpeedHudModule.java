package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Horizontal movement speed in blocks/second, sampled from position per tick. */
public final class SpeedHudModule extends HudModule {
    private double lastX;
    private double lastZ;
    private double speed;

    public SpeedHudModule() {
        super("speed_hud", "Speed", new HudPosition(HudAnchor.TOP_LEFT, 4, 40), false);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            lastX = player.getX();
            lastZ = player.getZ();
        }
        speed = 0;
    }

    @Override
    public void onTick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        double dx = player.getX() - lastX;
        double dz = player.getZ() - lastZ;
        lastX = player.getX();
        lastZ = player.getZ();
        speed = Math.sqrt(dx * dx + dz * dz) * 20.0; // 20 ticks per second
    }

    @Override
    protected String hudText() {
        if (Minecraft.getInstance().player == null) {
            return null;
        }
        return String.format(Locale.ROOT, "%.1f b/s", speed);
    }
}
