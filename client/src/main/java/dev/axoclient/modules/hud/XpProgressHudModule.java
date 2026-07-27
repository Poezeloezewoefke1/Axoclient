package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** How far along you are to the next experience level, as a percentage. */
public final class XpProgressHudModule extends HudModule {

    public XpProgressHudModule() {
        super("xp_progress_hud", "XP Progress", new HudPosition(HudAnchor.TOP_LEFT, 4, 112), false);
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        int percent = Math.round(player.experienceProgress * 100.0F);
        return "Next level " + percent + "%";
    }
}
