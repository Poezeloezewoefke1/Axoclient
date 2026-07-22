package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;

/** Name of the biome you're standing in. Opt-in. */
public final class BiomeHudModule extends HudModule {

    public BiomeHudModule() {
        super("biome_hud", "Biome", new HudPosition(HudAnchor.TOP_LEFT, 4, 76), false);
    }

    @Override
    protected String hudText() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return null;
        }
        return minecraft.level.getBiome(minecraft.player.blockPosition())
            .unwrapKey()
            .map(key -> prettify(key.location().getPath()))
            .orElse("Unknown");
    }

    private static String prettify(String path) {
        String spaced = path.replace('_', ' ');
        if (spaced.isEmpty()) {
            return spaced;
        }
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
