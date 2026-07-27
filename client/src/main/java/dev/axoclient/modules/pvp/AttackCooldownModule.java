package dev.axoclient.modules.pvp;

import dev.axoclient.core.ModuleCategory;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * How charged your next attack is, as a bar. Swinging at 100% lands full
 * damage; this just surfaces the value the vanilla attack indicator already
 * uses, in a form you can put where you actually look.
 */
public final class AttackCooldownModule extends HudModule {
    private static final int SEGMENTS = 10;

    public AttackCooldownModule() {
        super("attack_cooldown", "Attack Charge", ModuleCategory.PVP, new HudPosition(HudAnchor.BOTTOM_CENTER, 0, -40), false);
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        float charge = player.getAttackStrengthScale(0.0F);
        int filled = Math.round(charge * SEGMENTS);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < SEGMENTS; i++) {
            bar.append(i < filled ? '|' : '.');
        }
        return bar + " " + Math.round(charge * 100) + "%";
    }
}
