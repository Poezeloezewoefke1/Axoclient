package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Total armour and armour toughness as numbers. Toughness — what diamond and
 * netherite add on top — has no vanilla HUD at all, yet it decides how much
 * a big hit gets through.
 */
public final class ArmorPointsModule extends HudModule {

    public ArmorPointsModule() {
        super("armor_points", "Armor Points", new HudPosition(HudAnchor.TOP_LEFT, 4, 196), false);
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        double toughness = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        return String.format(Locale.ROOT, "Armor %d · toughness %.0f", player.getArmorValue(), toughness);
    }
}
