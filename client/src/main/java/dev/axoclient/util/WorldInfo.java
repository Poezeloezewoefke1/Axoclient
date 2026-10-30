package dev.axoclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.Level;

/**
 * Short, stable names for "which world am I in", used to keep per-world data
 * such as waypoints apart.
 *
 * Dimensions are compared against Level's constants rather than read through
 * ResourceKey#location(), which does not resolve on 1.21.11 (see
 * BiomeHudModule). Modded dimensions fall back to the key's toString().
 */
public final class WorldInfo {
    public static final String OVERWORLD = "overworld";
    public static final String NETHER = "nether";
    public static final String END = "end";

    private WorldInfo() {
    }

    /** "mp:" plus the address on a server, "sp:" plus the save name in singleplayer, null outside a world. */
    public static String worldId() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        ServerData server = minecraft.getCurrentServer();
        if (server != null) {
            return "mp:" + server.ip;
        }
        IntegratedServer local = minecraft.getSingleplayerServer();
        if (local != null) {
            return "sp:" + local.getWorldData().getLevelName();
        }
        return "unknown";
    }

    /** One of the constants above, a modded dimension's key, or null when not in a world. */
    public static String dimensionId() {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        if (level.dimension() == Level.OVERWORLD) {
            return OVERWORLD;
        }
        if (level.dimension() == Level.NETHER) {
            return NETHER;
        }
        if (level.dimension() == Level.END) {
            return END;
        }
        return level.dimension().toString();
    }
}
