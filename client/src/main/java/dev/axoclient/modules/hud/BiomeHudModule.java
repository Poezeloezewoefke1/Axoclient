package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * The biome you're standing in.
 *
 * An earlier attempt at this module went through ResourceKey#location(),
 * which doesn't resolve on 1.21.11, so it was removed. This version reads
 * the registry name straight off the Holder instead, which keeps ResourceKey
 * out of the picture entirely.
 */
public final class BiomeHudModule extends HudModule {

    public BiomeHudModule() {
        super("biome_hud", "Biome", new HudPosition(HudAnchor.TOP_LEFT, 4, 124), false);
    }

    @Override
    protected String hudText() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return null;
        }
        String registryName = minecraft.level.getBiome(player.blockPosition()).getRegisteredName();
        return prettify(registryName);
    }

    /** "minecraft:snowy_taiga" -> "Snowy Taiga". */
    private static String prettify(String registryName) {
        int colon = registryName.indexOf(':');
        String path = colon >= 0 ? registryName.substring(colon + 1) : registryName;
        StringBuilder out = new StringBuilder(path.length());
        boolean capitalise = true;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '_' || c == '/') {
                out.append(' ');
                capitalise = true;
            } else if (capitalise) {
                out.append(Character.toUpperCase(c));
                capitalise = false;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
