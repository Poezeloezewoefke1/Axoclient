package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Remaining durability of each worn armour piece, helmet first. Empty slots
 * are skipped, so the line shrinks to only what you're actually wearing.
 */
public final class ArmorHudModule extends HudModule {
    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    };

    public ArmorHudModule() {
        super("armor_hud", "Armor", new HudPosition(HudAnchor.TOP_LEFT, 4, 100), false);
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        StringBuilder line = new StringBuilder("Armor");
        boolean any = false;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            line.append(' ').append(durabilityPercent(stack)).append('%');
            any = true;
        }
        return any ? line.toString() : null;
    }

    /** 100 for indestructible/undamageable items, else remaining durability. */
    private static int durabilityPercent(ItemStack stack) {
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return 100;
        }
        return (int) Math.round((max - stack.getDamageValue()) * 100.0 / max);
    }
}
