package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Your armour and held items as real item icons, each with durability left
 * (or the stack size for things that don't wear out), coloured green to red.
 * The icon version of the text-only Armor module. Empty slots are skipped.
 *
 * Drawn as a column; the HUD editor drags it by its top row.
 */
public final class ArmorIconsModule extends HudModule {
    private static final int ROW = 17;
    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET,
        EquipmentSlot.MAINHAND,
        EquipmentSlot.OFFHAND
    };

    public ArmorIconsModule() {
        super("armor_icons", "Armor Icons", new HudPosition(HudAnchor.MIDDLE_LEFT, 8, 50), false);
    }

    @Override
    protected String hudText() {
        return null; // custom rendering below; the editor shows the module name
    }

    @Override
    public void renderHud(GuiGraphics graphics) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        if (stacks.isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int x = position().anchor().x(graphics.guiWidth(), 16, position().offsetX());
        int y = position().anchor().y(graphics.guiHeight(), stacks.size() * ROW, position().offsetY());
        for (ItemStack stack : stacks) {
            graphics.renderItem(stack, x, y);
            String label;
            int colour;
            if (stack.isDamageableItem() && stack.getMaxDamage() > 0) {
                int left = stack.getMaxDamage() - stack.getDamageValue();
                label = Integer.toString(left);
                colour = colourFor(left / (float) stack.getMaxDamage());
            } else {
                label = stack.getCount() > 1 ? Integer.toString(stack.getCount()) : "";
                colour = 0xFFFFFFFF;
            }
            if (!label.isEmpty()) {
                graphics.drawString(font, label, x + 19, y + (16 - font.lineHeight) / 2 + 1, colour, true);
            }
            y += ROW;
        }
    }

    /** Green when healthy, through yellow, to red near breaking. */
    private static int colourFor(float fraction) {
        if (fraction > 0.5F) {
            return 0xFF4ADE80;
        }
        if (fraction > 0.2F) {
            return 0xFFFACC15;
        }
        return 0xFFEF4444;
    }
}
