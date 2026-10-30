package dev.axoclient.modules.pvp;

import dev.axoclient.core.ModuleCategory;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * How many of the fight-deciding items you have left — totems, pearls,
 * golden apples, arrows — without opening your inventory. Types you have
 * none of are left off, so the line stays short.
 */
public final class ItemCounterModule extends HudModule {
    private record Tracked(String label, Item... items) {}

    private static final Tracked[] TRACKED = {
        new Tracked("Totems", Items.TOTEM_OF_UNDYING),
        new Tracked("Pearls", Items.ENDER_PEARL),
        new Tracked("Gapples", Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE),
        new Tracked("Arrows", Items.ARROW, Items.SPECTRAL_ARROW, Items.TIPPED_ARROW)
    };

    public ItemCounterModule() {
        super("item_counter", "Item Counter", ModuleCategory.PVP, new HudPosition(HudAnchor.TOP_RIGHT, -4, 76), false);
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        Inventory inventory = player.getInventory();
        StringBuilder line = new StringBuilder();
        for (Tracked tracked : TRACKED) {
            int count = count(inventory, tracked.items());
            if (count == 0) {
                continue;
            }
            if (line.length() > 0) {
                line.append(" · ");
            }
            line.append(tracked.label()).append(' ').append(count);
        }
        return line.length() > 0 ? line.toString() : null;
    }

    private static int count(Inventory inventory, Item[] items) {
        int total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            for (Item item : items) {
                if (stack.is(item)) {
                    total += stack.getCount();
                    break;
                }
            }
        }
        return total;
    }
}
