package dev.axoclient.modules.qol;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.gui.notify.Notification;
import dev.axoclient.gui.notify.Notifications;
import dev.axoclient.util.WorldInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * A notification the moment beds start working, so you can skip the night
 * before the first creeper arrives. Beds are usable from tick 12542 of the
 * day in clear weather, or at any time during a thunderstorm.
 */
public final class SleepReminderModule extends AxoModule {
    private static final long DAY_TICKS = 24000L;
    private static final long BEDS_FROM = 12542L;
    private static final long BEDS_UNTIL = 23460L;

    private Boolean couldSleep;

    public SleepReminderModule() {
        super("sleep_reminder", "Sleep Reminder", ModuleCategory.QOL, false);
    }

    @Override
    protected void onEnable() {
        couldSleep = null;
    }

    @Override
    public void onTick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !WorldInfo.OVERWORLD.equals(WorldInfo.dimensionId())) {
            couldSleep = null;
            return;
        }
        long time = Math.floorMod(level.getDayTime(), DAY_TICKS);
        boolean canSleep = level.isThundering() || (time >= BEDS_FROM && time < BEDS_UNTIL);
        // Only on the change, and never on the first tick in a world: joining
        // at night isn't news.
        if (couldSleep != null && canSleep && !couldSleep) {
            Notifications.push(
                level.isThundering() ? "Thunderstorm: you can sleep now" : "Night is falling: you can sleep now",
                Notification.Type.INFO
            );
        }
        couldSleep = canSleep;
    }
}
