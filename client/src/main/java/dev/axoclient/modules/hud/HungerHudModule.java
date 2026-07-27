package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Food level (0–20) on the HUD. */
public final class HungerHudModule extends HudModule {

    public HungerHudModule() {
        super("hunger_hud", "Hunger", new HudPosition(HudAnchor.TOP_LEFT, 4, 64), false);
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        return "Food " + player.getFoodData().getFoodLevel() + "/20";
    }
}
