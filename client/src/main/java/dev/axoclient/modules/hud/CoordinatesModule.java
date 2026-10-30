package dev.axoclient.modules.hud;

import dev.axoclient.core.ModuleSetting;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Player position + facing on the HUD (roadmap P1-08), anchor-positioned.
 *
 * Config (module "coordinates"):
 *   decimals — 0, 1 or 2 decimal places (1 is the original look)
 *   facing   — append the facing direction (On / Off)
 */
public final class CoordinatesModule extends HudModule {
    private static final String[] FORMATS = {"%.0f / %.0f / %.0f", "%.1f / %.1f / %.1f", "%.2f / %.2f / %.2f"};

    public CoordinatesModule() {
        super("coordinates", "Coordinates", new HudPosition(HudAnchor.TOP_LEFT, 4, 16));
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(
            ModuleSetting.choice(id(), "decimals", "Decimals", List.of("0", "1", "2"), 1),
            ModuleSetting.choice(id(), "facing", "Show facing", List.of("On", "Off"), 0)
        );
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        // %.0f rounds; whole-block coordinates should floor, like F3 shows.
        boolean whole = settings().get(0).get() == 0;
        String position = String.format(
            Locale.ROOT,
            FORMATS[settings().get(0).get()],
            whole ? Math.floor(player.getX()) : player.getX(),
            whole ? Math.floor(player.getY()) : player.getY(),
            whole ? Math.floor(player.getZ()) : player.getZ()
        );
        if (settings().get(1).get() != 0) {
            return position;
        }
        return position + "  " + player.getDirection().toString().toUpperCase(Locale.ROOT);
    }
}
