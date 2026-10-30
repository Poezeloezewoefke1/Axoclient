package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import dev.axoclient.util.OreDepths;
import dev.axoclient.util.WorldInfo;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Your Y level and which ores are richest at it, so you strip-mine at the
 * right height. The bands live in {@link OreDepths}.
 */
public final class DepthGuideModule extends HudModule {

    public DepthGuideModule() {
        super("depth_guide", "Depth Guide", new HudPosition(HudAnchor.TOP_LEFT, 4, 172), false);
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        int y = (int) Math.floor(player.getY());
        List<String> ores = OreDepths.bestAt(WorldInfo.dimensionId(), y);
        return ores.isEmpty() ? "Y " + y : "Y " + y + " · best for " + String.join(", ", ores);
    }
}
