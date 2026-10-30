package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Says so when there's no empty slot left, before you mine another ten ores
 * that silently stay on the floor.
 */
public final class InventoryFullModule extends HudModule {

    public InventoryFullModule() {
        super("inventory_full", "Inventory Full", new HudPosition(HudAnchor.BOTTOM_CENTER, 0, -54), false);
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isCreative()) {
            return null;
        }
        return player.getInventory().getFreeSlot() < 0 ? "Inventory full" : null;
    }
}
