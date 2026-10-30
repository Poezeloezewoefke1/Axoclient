package dev.axoclient.modules.hud;

import dev.axoclient.core.ModuleSetting;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Tells you to eat before it matters. At 6 food or less you can't sprint,
 * which is the moment that usually catches people out, so that gets its own
 * message.
 *
 * Config (module "hunger_warning"):
 *   food — warn at or below this food level
 */
public final class HungerWarningModule extends HudModule {
    private static final int CANT_SPRINT_AT = 6;

    public HungerWarningModule() {
        super("hunger_warning", "Hunger Warning", new HudPosition(HudAnchor.BOTTOM_CENTER, 0, -90), false);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(ModuleSetting.plain(id(), "food", "Warn at food", 1, 19, 10));
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isCreative() || player.isSpectator()) {
            return null;
        }
        int food = player.getFoodData().getFoodLevel();
        if (food <= CANT_SPRINT_AT) {
            return "Too hungry to sprint, eat something";
        }
        return food <= settings().get(0).get() ? "Getting hungry" : null;
    }
}
