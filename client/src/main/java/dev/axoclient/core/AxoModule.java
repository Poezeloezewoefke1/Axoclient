package dev.axoclient.core;

/**
 * Base class for every user-facing feature. One subclass per feature,
 * registered once in {@link dev.axoclient.AxoClient}. Enabled-state is
 * owned by {@link ModuleManager} so persistence stays in one place.
 */
public abstract class AxoModule {
    private final String id;
    private final String displayName;
    private final ModuleCategory category;
    private final boolean enabledByDefault;

    protected AxoModule(String id, String displayName, ModuleCategory category, boolean enabledByDefault) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.enabledByDefault = enabledByDefault;
    }

    public final String id() {
        return id;
    }

    public final String displayName() {
        return displayName;
    }

    public final ModuleCategory category() {
        return category;
    }

    public final boolean enabledByDefault() {
        return enabledByDefault;
    }

    /** Called when the module transitions to enabled (including at startup). */
    protected void onEnable() {
    }

    /** Called when the module transitions to disabled. Must restore any state it changed. */
    protected void onDisable() {
    }

    /** Called once per client tick while enabled (wired in roadmap task P1-03). */
    public void onTick() {
    }

    /** Default GLFW toggle key, or -1 for none. Overridable; users can rebind. */
    public int defaultToggleKey() {
        return -1;
    }
}
