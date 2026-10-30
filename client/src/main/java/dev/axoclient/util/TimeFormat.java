package dev.axoclient.util;

import java.util.Locale;

/** Shared duration formatting for timers and countdowns. Pure, unit-tested. */
public final class TimeFormat {
    private TimeFormat() {
    }

    /** "1:05" under an hour, "1:02:05" past it. Negative input reads as zero. */
    public static String clock(long millis) {
        long totalSeconds = Math.max(0, millis) / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds / 60) % 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
    }

    /** {@link #clock} plus tenths of a second, for a stopwatch: "1:05.3". */
    public static String stopwatch(long millis) {
        long safe = Math.max(0, millis);
        return clock(safe) + "." + (safe % 1000) / 100;
    }
}
