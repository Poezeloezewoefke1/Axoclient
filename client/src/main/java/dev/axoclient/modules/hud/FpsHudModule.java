package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;

/** FPS counter — the module system's proof-of-life, now anchor-positioned (P1-06). */
public final class FpsHudModule extends HudModule {

    public FpsHudModule() {
        super("fps_hud", "FPS Counter", new HudPosition(HudAnchor.TOP_LEFT, 4, 4));
    }

    @Override
    protected String hudText() {
        return Minecraft.getInstance().getFps() + " fps";
    }
}
