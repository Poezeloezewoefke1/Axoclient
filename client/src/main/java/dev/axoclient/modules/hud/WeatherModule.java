package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Current weather in a word. Useful underground, where you can't see that a
 * thunderstorm has started — and thunderstorms let monsters spawn in daylight.
 */
public final class WeatherModule extends HudModule {

    public WeatherModule() {
        super("weather", "Weather", new HudPosition(HudAnchor.TOP_RIGHT, -4, 88), false);
    }

    @Override
    protected String hudText() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        if (level.isThundering()) {
            return "Thunderstorm";
        }
        return level.isRaining() ? "Raining" : "Clear";
    }
}
