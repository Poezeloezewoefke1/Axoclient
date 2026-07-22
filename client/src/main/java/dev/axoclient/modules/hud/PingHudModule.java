package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

/** Your latency to the current server, in milliseconds. Hidden in singleplayer. */
public final class PingHudModule extends HudModule {

    public PingHudModule() {
        super("ping_hud", "Ping", new HudPosition(HudAnchor.TOP_LEFT, 4, 52), false);
    }

    @Override
    protected String hudText() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (minecraft.player == null || connection == null) {
            return null;
        }
        PlayerInfo info = connection.getPlayerInfo(minecraft.player.getUUID());
        if (info == null) {
            return null;
        }
        return info.getLatency() + " ms";
    }
}
