package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;

/**
 * Which server you're on and how busy it is. Hidden in singleplayer, where
 * neither number means anything.
 */
public final class ServerInfoHudModule extends HudModule {

    public ServerInfoHudModule() {
        super("server_info_hud", "Server Info", new HudPosition(HudAnchor.TOP_RIGHT, -4, 40), false);
    }

    @Override
    protected String hudText() {
        Minecraft minecraft = Minecraft.getInstance();
        ServerData server = minecraft.getCurrentServer();
        ClientPacketListener connection = minecraft.getConnection();
        if (server == null || connection == null) {
            return null;
        }
        String name = server.name != null && !server.name.isEmpty() ? server.name : server.ip;
        return name + " · " + connection.getOnlinePlayers().size() + " online";
    }
}
