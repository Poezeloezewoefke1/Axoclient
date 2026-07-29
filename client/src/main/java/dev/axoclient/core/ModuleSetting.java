package dev.axoclient.core;

/**
 * One adjustable number belonging to a module, shown in the ClickGUI so
 * nobody has to hand-edit axoclient.json to change a crosshair size.
 *
 * Integers only, on purpose. Every knob worth exposing is a count, a size or
 * a speed, and one widget type means one code path to get right — fractional
 * values are carried as tenths ({@link Format#TENTHS}) instead of introducing
 * a second type.
 */
public final class ModuleSetting {
    /** How the stored integer is rendered to the player. */
    public enum Format {
        /** As-is: "4". */
        PLAIN,
        /** Stored in tenths, shown as "1.4". */
        TENTHS,
        /** Ticks, shown as seconds: "0.25s". */
        TICKS
    }

    private final String moduleId;
    private final String key;
    private final String label;
    private final int min;
    private final int max;
    private final int step;
    private final int fallback;
    private final Format format;

    public ModuleSetting(
        String moduleId,
        String key,
        String label,
        int min,
        int max,
        int step,
        int fallback,
        Format format
    ) {
        this.moduleId = moduleId;
        this.key = key;
        this.label = label;
        this.min = min;
        this.max = max;
        this.step = Math.max(1, step);
        this.fallback = fallback;
        this.format = format;
    }

    public static ModuleSetting plain(
        String moduleId, String key, String label, int min, int max, int fallback
    ) {
        return new ModuleSetting(moduleId, key, label, min, max, 1, fallback, Format.PLAIN);
    }

    public String label() {
        return label;
    }

    public int get() {
        int stored = ModuleManager.get().config().getModuleInt(moduleId, key, fallback);
        return clamp(stored);
    }

    /** Move by one step in the given direction (-1 or +1), clamped to range. */
    public void nudge(int direction) {
        ModuleManager.get().config().setModuleInt(moduleId, key, clamp(get() + step * direction));
    }

    /** Text shown next to the label. */
    public String display() {
        int value = get();
        return switch (format) {
            case TENTHS -> String.format("%.1f", value / 10.0);
            case TICKS -> String.format("%.2fs", value / 20.0);
            case PLAIN -> Integer.toString(value);
        };
    }

    private int clamp(int value) {
        return Math.max(min, Math.min(max, value));
    }
}
