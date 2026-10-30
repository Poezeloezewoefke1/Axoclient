package dev.axoclient.modules.pvp;

import dev.axoclient.core.ModuleCategory;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Ender pearl cooldown as a percentage, so you know when the next throw is
 * ready without staring at the greyed-out hotbar slot. Hidden when ready.
 */
public final class PearlCooldownModule extends HudModule {
    /** Built on first use: item stacks shouldn't be made during mod init. */
    private ItemStack pearl;

    public PearlCooldownModule() {
        super("pearl_cooldown", "Pearl Cooldown", ModuleCategory.PVP, new HudPosition(HudAnchor.CENTER, 0, 46), false);
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        if (pearl == null) {
            pearl = new ItemStack(Items.ENDER_PEARL);
        }
        float remaining = player.getCooldowns().getCooldownPercent(pearl, 0.0F);
        return remaining > 0.0F ? "Pearl " + Math.round(remaining * 100) + "%" : null;
    }
}
