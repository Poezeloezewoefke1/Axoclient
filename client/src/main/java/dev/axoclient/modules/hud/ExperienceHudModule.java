package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Current experience level on the HUD. */
public final class ExperienceHudModule extends HudModule {

    public ExperienceHudModule() {
        super("experience_hud", "Experience", new HudPosition(HudAnchor.TOP_LEFT, 4, 76), false);
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        return "XP " + player.experienceLevel;
    }
}
