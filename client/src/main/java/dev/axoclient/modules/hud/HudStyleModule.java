package dev.axoclient.modules.hud;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.core.ModuleSetting;
import dev.axoclient.gui.theme.Themes;
import java.util.List;

/**
 * One place to restyle every text HUD element: colour, a dark backing box
 * for readability over bright terrain, and text shadow. When this module is
 * off the HUD looks exactly as it always has (Axo blue, shadow, no box).
 *
 * Read by {@link dev.axoclient.hud.HudModule#renderHud} each frame through
 * the static accessors below, so no HUD module needs to know it exists.
 */
public final class HudStyleModule extends AxoModule {
    public static final String ID = "hud_style";

    private static final int AXO_BLUE = 0xFF38BDF8;
    private static final List<String> COLOUR_NAMES = List.of("Axo Blue", "Theme Accent", "White", "Gold", "Mint", "Pink");
    private static final int[] COLOURS = {AXO_BLUE, 0, 0xFFFFFFFF, 0xFFFBBF24, 0xFF34D399, 0xFFF472B6};
    private static final List<String> ON_OFF = List.of("On", "Off");

    public HudStyleModule() {
        super(ID, "HUD Style", ModuleCategory.HUD, false);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(
            ModuleSetting.choice(ID, "colour", "Text colour", COLOUR_NAMES, 0),
            ModuleSetting.choice(ID, "background", "Background", ON_OFF, 0),
            ModuleSetting.choice(ID, "shadow", "Text shadow", ON_OFF, 0)
        );
    }

    private static boolean active() {
        return ModuleManager.get().byId(ID).map(ModuleManager.get()::isEnabled).orElse(false);
    }

    private static int setting(String key) {
        return ModuleManager.get().config().getModuleInt(ID, key, 0);
    }

    /** ARGB colour for HUD text. */
    public static int textColour() {
        if (!active()) {
            return AXO_BLUE;
        }
        int index = Math.floorMod(setting("colour"), COLOURS.length);
        return index == 1 ? Themes.current().accent : COLOURS[index];
    }

    public static boolean background() {
        return active() && setting("background") == 0;
    }

    public static boolean shadow() {
        return !active() || setting("shadow") == 0;
    }
}
