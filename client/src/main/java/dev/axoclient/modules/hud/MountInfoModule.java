package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * What you're riding, its health and how fast it is actually going. Speed is
 * measured from movement rather than read from attributes, so it is the true
 * figure for any mount — horse, pig, strider, camel or a boat.
 */
public final class MountInfoModule extends HudModule {
    private Entity tracked;
    private double lastX;
    private double lastZ;
    private double speed;

    public MountInfoModule() {
        super("mount_info", "Mount Info", new HudPosition(HudAnchor.BOTTOM_CENTER, 0, -78), false);
    }

    @Override
    public void onTick() {
        LocalPlayer player = Minecraft.getInstance().player;
        Entity vehicle = player == null ? null : player.getVehicle();
        if (vehicle == null) {
            tracked = null;
            return;
        }
        if (vehicle != tracked) {
            tracked = vehicle;
            speed = 0;
        } else {
            double dx = vehicle.getX() - lastX;
            double dz = vehicle.getZ() - lastZ;
            speed = Math.sqrt(dx * dx + dz * dz) * 20.0;
        }
        lastX = vehicle.getX();
        lastZ = vehicle.getZ();
    }

    @Override
    protected String hudText() {
        if (tracked == null) {
            return null;
        }
        String line = String.format(Locale.ROOT, "%s %.1f b/s", tracked.getName().getString(), speed);
        if (tracked instanceof LivingEntity living) {
            line += String.format(Locale.ROOT, " · %.0f/%.0f HP", living.getHealth(), living.getMaxHealth());
        }
        return line;
    }
}
