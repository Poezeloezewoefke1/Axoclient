package dev.axoclient.modules.hud;

import dev.axoclient.core.ModuleSetting;
import dev.axoclient.gui.notify.Notification;
import dev.axoclient.gui.notify.Notifications;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Current weather in a word. Useful underground, where you can't see that a
 * thunderstorm has started — and thunderstorms let monsters spawn in daylight.
 * Optionally announces rain and thunder as they start.
 *
 * Config (module "weather"):
 *   notify — notification when rain or a thunderstorm starts (On / Off)
 */
public final class WeatherModule extends HudModule {
    private String lastWeather;

    public WeatherModule() {
        super("weather", "Weather", new HudPosition(HudAnchor.TOP_RIGHT, -4, 88), false);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(ModuleSetting.choice(id(), "notify", "Notify on change", List.of("On", "Off"), 0));
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        lastWeather = null;
    }

    @Override
    public void onTick() {
        String weather = hudText();
        // Not on the first reading: joining a world mid-storm isn't a change.
        if (lastWeather != null && weather != null && !weather.equals(lastWeather)
            && !"Clear".equals(weather) && settings().get(0).get() == 0) {
            Notifications.push(
                "Thunderstorm".equals(weather) ? "A thunderstorm has started" : "It has started raining",
                Notification.Type.INFO
            );
        }
        lastWeather = weather;
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
