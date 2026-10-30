package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import dev.axoclient.util.WorldInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;

/**
 * Block and sky light where you stand, for lighting up a base properly.
 *
 * Overworld monsters only spawn at block light 0, so that is when the line
 * says so. The rule is different in the Nether and the End, so the warning
 * is left off there rather than being wrong.
 */
public final class LightLevelModule extends HudModule {

    public LightLevelModule() {
        super("light_level", "Light Level", new HudPosition(HudAnchor.TOP_RIGHT, -4, 64), false);
    }

    @Override
    protected String hudText() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return null;
        }
        BlockPos pos = player.blockPosition();
        int block = minecraft.level.getBrightness(LightLayer.BLOCK, pos);
        int sky = minecraft.level.getBrightness(LightLayer.SKY, pos);
        String line = "Light " + block + " · sky " + sky;
        if (block == 0 && WorldInfo.OVERWORLD.equals(WorldInfo.dimensionId())) {
            line += " · mobs can spawn";
        }
        return line;
    }
}
