package dev.axoclient.modules.pvp;

import dev.axoclient.core.ModuleCategory;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;

/**
 * Warns when you carry totems but none is in your hands, typically right
 * after one popped. A totem only saves you from a hand slot. Silent if you
 * carry no totems at all, so it never nags players who don't use them.
 */
public final class TotemWarningModule extends HudModule {

    public TotemWarningModule() {
        super("totem_warning", "Totem Warning", ModuleCategory.PVP, new HudPosition(HudAnchor.CENTER, 0, 58), false);
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        if (player.getOffhandItem().is(Items.TOTEM_OF_UNDYING) || player.getMainHandItem().is(Items.TOTEM_OF_UNDYING)) {
            return null;
        }
        int carried = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(Items.TOTEM_OF_UNDYING)) {
                carried += inventory.getItem(slot).getCount();
            }
        }
        return carried > 0 ? "No totem in hand (" + carried + " in inventory)" : null;
    }
}
