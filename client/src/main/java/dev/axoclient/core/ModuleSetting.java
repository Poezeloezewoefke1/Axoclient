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
        TICKS,
        /** Index into {@link #choices}, shown as that entry's label. */
        CHOICE,
        /** A GLFW key code, shown as its key name and rebound by pressing a key. */
        KEY
    }

    private final String moduleId;
    private final String key;
    private final String label;
    private final int min;
    private final int max;
    private final int step;
    private final int fallback;
    private final Format format;
    /** Labels for CHOICE settings; empty for every other format. */
    private final java.util.List<String> choices;

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
        this(moduleId, key, label, min, max, step, fallback, format, java.util.List.of());
    }

    private ModuleSetting(
        String moduleId,
        String key,
        String label,
        int min,
        int max,
        int step,
        int fallback,
        Format format,
        java.util.List<String> choices
    ) {
        this.moduleId = moduleId;
        this.key = key;
        this.label = label;
        this.min = min;
        this.max = max;
        this.step = Math.max(1, step);
        this.fallback = fallback;
        this.format = format;
        this.choices = java.util.List.copyOf(choices);
    }

    public static ModuleSetting plain(
        String moduleId, String key, String label, int min, int max, int fallback
    ) {
        return new ModuleSetting(moduleId, key, label, min, max, 1, fallback, Format.PLAIN);
    }

    /**
     * A pick-one-from-a-list setting, stored as the index. This is what turns
     * "twenty separate trail modules" into one module with a Style knob.
     */
    public static ModuleSetting choice(
        String moduleId, String key, String label, java.util.List<String> names, int fallbackIndex
    ) {
        return new ModuleSetting(
            moduleId, key, label, 0, Math.max(0, names.size() - 1), 1, fallbackIndex, Format.CHOICE, names
        );
    }

    /** A rebindable key, stored as its GLFW code. 0 means unbound. */
    public static ModuleSetting key(String moduleId, String key, String label, int fallbackGlfwKey) {
        return new ModuleSetting(
            moduleId, key, label, 0, GLFW_LAST_KEY, 1, fallbackGlfwKey, Format.KEY, java.util.List.of()
        );
    }

    /** GLFW_KEY_LAST. Hard-coded to keep this class free of an LWJGL import. */
    private static final int GLFW_LAST_KEY = 348;

    public Format format() {
        return format;
    }

    public java.util.List<String> choices() {
        return choices;
    }

    /** Directly set the stored value — used by the key-capture control. */
    public void set(int value) {
        ModuleManager.get().config().setModuleInt(moduleId, key, clamp(value));
    }

    public String label() {
        return label;
    }

    public int get() {
        int stored = ModuleManager.get().config().getModuleInt(moduleId, key, fallback);
        return clamp(stored);
    }

    /**
     * Move by one step in the given direction (-1 or +1). Numeric settings
     * clamp at the ends; CHOICE wraps, because a list of cape colours has no
     * meaningful "first" or "last" to get stuck against.
     */
    public void nudge(int direction) {
        if (format == Format.CHOICE && !choices.isEmpty()) {
            int next = Math.floorMod(get() + direction, choices.size());
            ModuleManager.get().config().setModuleInt(moduleId, key, next);
            return;
        }
        ModuleManager.get().config().setModuleInt(moduleId, key, clamp(get() + step * direction));
    }

    /** Text shown next to the label. */
    public String display() {
        int value = get();
        return switch (format) {
            case TENTHS -> String.format("%.1f", value / 10.0);
            case TICKS -> String.format("%.2fs", value / 20.0);
            case PLAIN -> Integer.toString(value);
            case CHOICE -> choices.isEmpty() ? Integer.toString(value) : choices.get(clamp(value));
            case KEY -> value <= 0 ? "None" : dev.axoclient.input.Keybinds.keyName(value);
        };
    }

    private int clamp(int value) {
        return Math.max(min, Math.min(max, value));
    }
}
