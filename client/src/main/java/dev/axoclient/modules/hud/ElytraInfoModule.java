package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * While gliding: air speed, pitch and how much elytra you have left, which
 * is exactly what decides whether a long flight makes it.
 */
public final class ElytraInfoModule extends HudModule {

    public ElytraInfoModule() {
        super("elytra_info", "Elytra Info", new HudPosition(HudAnchor.BOTTOM_CENTER, 0, -66), false);
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isFallFlying()) {
            return null;
        }
        double speed = player.getDeltaMovement().length() * 20.0;
        StringBuilder line = new StringBuilder(String.format(
            Locale.ROOT, "Gliding %.0f b/s · pitch %.0f", speed, player.getXRot()
        ));
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        int max = chest.getMaxDamage();
        if (max > 0) {
            line.append(" · ").append(Math.round((max - chest.getDamageValue()) * 100.0 / max)).append('%');
        }
        return line.toString();
    }
}
