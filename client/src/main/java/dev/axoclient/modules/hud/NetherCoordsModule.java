package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import dev.axoclient.util.WorldInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Where you'd be in the other dimension: divided by 8 in the Overworld,
 * multiplied by 8 in the Nether. Saves the calculator trip when lining up a
 * portal. Hidden in the End and modded dimensions, where there is no pair.
 */
public final class NetherCoordsModule extends HudModule {
    private static final int SCALE = 8;

    public NetherCoordsModule() {
        super("nether_coords", "Nether Coords", new HudPosition(HudAnchor.TOP_LEFT, 4, 136), false);
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        String dim = WorldInfo.dimensionId();
        if (player == null || dim == null) {
            return null;
        }
        int x = (int) Math.floor(player.getX());
        int z = (int) Math.floor(player.getZ());
        if (WorldInfo.OVERWORLD.equals(dim)) {
            return "Nether " + Math.floorDiv(x, SCALE) + ", " + Math.floorDiv(z, SCALE);
        }
        if (WorldInfo.NETHER.equals(dim)) {
            return "Overworld " + x * SCALE + ", " + z * SCALE;
        }
        return null;
    }
}
