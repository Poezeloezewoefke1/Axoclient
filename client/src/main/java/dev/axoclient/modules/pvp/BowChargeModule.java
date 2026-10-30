package dev.axoclient.modules.pvp;

import dev.axoclient.core.ModuleCategory;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BowItem;

/**
 * How drawn your bow is, using the same power curve the game uses to fire,
 * so "100%" is exactly when a release becomes a full-power (critical) shot.
 */
public final class BowChargeModule extends HudModule {

    public BowChargeModule() {
        super("bow_charge", "Bow Charge", ModuleCategory.PVP, new HudPosition(HudAnchor.CENTER, 0, -24), false);
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isUsingItem() || !(player.getUseItem().getItem() instanceof BowItem)) {
            return null;
        }
        float power = BowItem.getPowerForTime(player.getTicksUsingItem());
        return power >= 1.0F ? "Bow FULL" : "Bow " + Math.round(power * 100) + "%";
    }
}
