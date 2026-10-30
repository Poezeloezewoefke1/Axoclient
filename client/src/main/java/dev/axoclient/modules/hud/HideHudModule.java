package dev.axoclient.modules.hud;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;

/**
 * While on, every Axo HUD element is hidden; vanilla's HUD is left alone
 * (F1 still hides everything). Bind it a key in the menu to flip all your
 * overlays off for a screenshot or a recording and back on again.
 *
 * Has no behaviour of its own: {@link dev.axoclient.core.ModuleManager#renderHud}
 * checks whether it's enabled.
 */
public final class HideHudModule extends AxoModule {
    public static final String ID = "hide_hud";

    public HideHudModule() {
        super(ID, "Hide Axo HUD", ModuleCategory.HUD, false);
    }
}
