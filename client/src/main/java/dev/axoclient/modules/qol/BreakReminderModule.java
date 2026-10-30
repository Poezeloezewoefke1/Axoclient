package dev.axoclient.modules.qol;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleSetting;
import dev.axoclient.gui.notify.Notification;
import dev.axoclient.gui.notify.Notifications;
import java.util.List;
import net.minecraft.client.Minecraft;

/**
 * A gentle nudge to stand up, drink some water and rest your eyes after a
 * set stretch of play. Only time spent in a world counts.
 *
 * Config (module "break_reminder"):
 *   minutes — play time between reminders
 */
public final class BreakReminderModule extends AxoModule {
    private static final int DEFAULT_MINUTES = 60;
    private static final int TICKS_PER_MINUTE = 20 * 60;

    private int ticksPlayed;

    public BreakReminderModule() {
        super("break_reminder", "Break Reminder", ModuleCategory.QOL, false);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(
            new ModuleSetting(id(), "minutes", "Every (minutes)", 15, 180, 15, DEFAULT_MINUTES, ModuleSetting.Format.PLAIN)
        );
    }

    @Override
    protected void onEnable() {
        ticksPlayed = 0;
    }

    @Override
    public void onTick() {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        int minutes = settings().get(0).get();
        if (++ticksPlayed >= minutes * TICKS_PER_MINUTE) {
            ticksPlayed = 0;
            Notifications.push(
                "You've played " + minutes + " minutes. Time for a stretch and some water?",
                Notification.Type.WARN
            );
        }
    }
}
