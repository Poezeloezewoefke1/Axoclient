package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Shouts when a worn or held item is nearly broken, so a good pickaxe or a
 * full set of armour doesn't shatter mid-fight. Silent until something is
 * actually in danger.
 */
public final class DurabilityWarningModule extends HudModule {
    /** Warn from this fraction of durability remaining. */
    private static final double WARN_AT = 0.10;

    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET,
        EquipmentSlot.MAINHAND
    };

    public DurabilityWarningModule() {
        super("durability_warning", "Durability Warning", new HudPosition(HudAnchor.TOP_CENTER, 0, 34), false);
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        int worst = 101;
        String worstName = null;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            int max = stack.getMaxDamage();
            if (stack.isEmpty() || max <= 0) {
                continue;
            }
            int percent = (int) Math.round((max - stack.getDamageValue()) * 100.0 / max);
            if (percent <= WARN_AT * 100 && percent < worst) {
                worst = percent;
                worstName = stack.getHoverName().getString();
            }
        }
        return worstName == null ? null : "! " + worstName + " " + worst + "%";
    }
}
