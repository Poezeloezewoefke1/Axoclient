package dev.axoclient.modules.pvp;

import dev.axoclient.core.ModuleCategory;
import dev.axoclient.gui.notify.Notification;
import dev.axoclient.gui.notify.Notifications;
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
 *
 * Also announces when a totem is used up, with how many you have left. A
 * drop in your totem count while no screen is open is treated as a pop —
 * moving totems around in your inventory happens with a screen open, so it
 * doesn't count.
 */
public final class TotemWarningModule extends HudModule {

    private int lastTotems = -1;

    public TotemWarningModule() {
        super("totem_warning", "Totem Warning", ModuleCategory.PVP, new HudPosition(HudAnchor.CENTER, 0, 58), false);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        lastTotems = -1;
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            lastTotems = -1;
            return;
        }
        int totems = countTotems(player.getInventory());
        if (minecraft.screen == null && lastTotems > totems && player.getHealth() > 0) {
            Notifications.push(
                "Totem used! " + totems + " left",
                totems == 0 ? Notification.Type.WARN : Notification.Type.INFO
            );
        }
        lastTotems = totems;
    }

    private static int countTotems(Inventory inventory) {
        int total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(Items.TOTEM_OF_UNDYING)) {
                total += inventory.getItem(slot).getCount();
            }
        }
        return total;
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
        int carried = countTotems(player.getInventory());
        return carried > 0 ? "No totem in hand (" + carried + " in inventory)" : null;
    }
}
