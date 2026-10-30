package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Which chunk you're in, where in it, and the region file it lives in —
 * for chunk-aligned farms, slime chunks and finding your base on disk.
 * Plain arithmetic on block coordinates, so it can't drift from the game.
 */
public final class ChunkInfoModule extends HudModule {

    public ChunkInfoModule() {
        super("chunk_info", "Chunk Info", new HudPosition(HudAnchor.TOP_LEFT, 4, 160), false);
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        int x = (int) Math.floor(player.getX());
        int z = (int) Math.floor(player.getZ());
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        return "Chunk " + chunkX + ", " + chunkZ
            + " · in " + (x & 15) + ", " + (z & 15)
            + " · r." + (chunkX >> 5) + "." + (chunkZ >> 5);
    }
}
