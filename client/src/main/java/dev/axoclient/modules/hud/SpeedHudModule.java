package dev.axoclient.modules.hud;

import dev.axoclient.core.ModuleSetting;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Horizontal movement speed, sampled from position per tick.
 *
 * Config (module "speed_hud"):
 *   unit — blocks per second or km/h (one block is one metre)
 */
public final class SpeedHudModule extends HudModule {
    private double lastX;
    private double lastZ;
    private double speed;

    public SpeedHudModule() {
        super("speed_hud", "Speed", new HudPosition(HudAnchor.TOP_LEFT, 4, 40), false);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(ModuleSetting.choice(id(), "unit", "Unit", List.of("Blocks/s", "km/h"), 0));
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
        if (settings().get(0).get() == 1) {
            return String.format(Locale.ROOT, "%.1f km/h", speed * 3.6);
        }
        return String.format(Locale.ROOT, "%.1f b/s", speed);
    }
}
