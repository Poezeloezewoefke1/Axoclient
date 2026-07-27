package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Cardinal facing direction from the player's yaw. Minecraft yaw is 0° at
 * south and increases clockwise, so the 8-wind table starts at south.
 */
public final class DirectionHudModule extends HudModule {
    private static final String[] DIRS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    public DirectionHudModule() {
        super("direction_hud", "Direction", new HudPosition(HudAnchor.TOP_LEFT, 4, 88), false);
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        float yaw = player.getYRot() % 360.0F;
        if (yaw < 0.0F) {
            yaw += 360.0F;
        }
        int index = (int) Math.floor((yaw + 22.5F) / 45.0F) & 7;
        return "Facing " + DIRS[index];
    }
}
