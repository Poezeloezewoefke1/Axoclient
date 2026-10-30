package dev.axoclient.modules.qol;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.gui.notify.Notification;
import dev.axoclient.gui.notify.Notifications;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * A heads-up ten seconds before a potion effect runs out, so Fire Resistance
 * doesn't end mid-lava-lake. Warns once per effect, when its timer crosses
 * the line; infinite effects (beacons, some servers) never trigger it.
 */
public final class EffectExpiryModule extends AxoModule {
    private static final int WARN_TICKS = 200;

    /** Last seen remaining ticks, by effect name. */
    private final Map<String, Integer> lastDuration = new HashMap<>();

    public EffectExpiryModule() {
        super("effect_expiry", "Effect Expiry Warning", ModuleCategory.QOL, false);
    }

    @Override
    protected void onEnable() {
        lastDuration.clear();
    }

    @Override
    public void onTick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            lastDuration.clear();
            return;
        }
        Map<String, Integer> seen = new HashMap<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            int duration = effect.getDuration();
            if (duration < 0) {
                continue;
            }
            String name = effect.getEffect().value().getDisplayName().getString();
            Integer before = lastDuration.get(name);
            if (before != null && before > WARN_TICKS && duration <= WARN_TICKS) {
                Notifications.push(name + " wears off in " + (duration / 20) + "s", Notification.Type.WARN);
            }
            seen.put(name, duration);
        }
        lastDuration.clear();
        lastDuration.putAll(seen);
    }
}
