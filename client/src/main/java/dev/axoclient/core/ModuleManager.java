package dev.axoclient.core;

import dev.axoclient.AxoClient;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Central module registry. Owns enabled-state and writes changes through
 * to {@link AxoConfig} so toggles always persist.
 */
public final class ModuleManager {
    private static ModuleManager instance;

    private final AxoConfig config;
    private final Map<String, AxoModule> modules = new LinkedHashMap<>();
    private final Map<String, Boolean> enabled = new LinkedHashMap<>();

    private ModuleManager(AxoConfig config) {
        this.config = config;
    }

    public static ModuleManager init(AxoConfig config) {
        instance = new ModuleManager(config);
        return instance;
    }

    public static ModuleManager get() {
        if (instance == null) {
            throw new IllegalStateException("ModuleManager accessed before init");
        }
        return instance;
    }

    public AxoConfig config() {
        return config;
    }

    public void register(AxoModule module) {
        if (modules.putIfAbsent(module.id(), module) != null) {
            throw new IllegalArgumentException("Duplicate module id: " + module.id());
        }
        boolean state = config.isEnabled(module.id(), module.enabledByDefault());
        enabled.put(module.id(), state);
        // NB: do NOT call onEnable() here. Registration runs during mod init,
        // before Minecraft.options / the world exist, so a module that touches
        // them (e.g. Fullbright reading options.gamma()) would NPE and crash the
        // game on boot. Persisted-enabled modules are activated later, once the
        // client has started, via enableInitial().
    }

    /**
     * Fire onEnable() for every module whose persisted state is enabled. Call
     * once the client is fully started (ClientLifecycleEvents.CLIENT_STARTED),
     * when Minecraft.options and the render system are safely initialized.
     */
    public void enableInitial() {
        for (AxoModule module : modules.values()) {
            if (isEnabled(module)) {
                try {
                    module.onEnable();
                } catch (Exception | LinkageError e) {
                    quarantine(module, "startup", e);
                }
            }
        }
    }

    public Collection<AxoModule> all() {
        return modules.values();
    }

    public Optional<AxoModule> byId(String id) {
        return Optional.ofNullable(modules.get(id));
    }

    public boolean isEnabled(AxoModule module) {
        return enabled.getOrDefault(module.id(), false);
    }

    public void setEnabled(AxoModule module, boolean state) {
        Boolean previous = enabled.put(module.id(), state);
        if (previous != null && previous == state) {
            return;
        }
        try {
            if (state) {
                module.onEnable();
            } else {
                module.onDisable();
            }
        } catch (Exception | LinkageError e) {
            quarantine(module, state ? "enable" : "disable", e);
            return;
        }
        config.setEnabled(module.id(), state);
        dev.axoclient.gui.notify.Notifications.push(
            module.displayName() + (state ? " enabled" : " disabled"),
            state ? dev.axoclient.gui.notify.Notification.Type.SUCCESS
                  : dev.axoclient.gui.notify.Notification.Type.INFO
        );
    }

    public void toggle(AxoModule module) {
        setEnabled(module, !isEnabled(module));
    }

    /** Dispatched from the client tick event (roadmap task P1-03). */
    public void tickAll() {
        for (AxoModule module : modules.values()) {
            if (isEnabled(module)) {
                try {
                    module.onTick();
                } catch (Exception | LinkageError e) {
                    quarantine(module, "tick", e);
                }
            }
        }
    }

    /**
     * Turns off a module that threw, instead of letting the exception crash
     * the game. Ticks and HUD frames run every few milliseconds, so one bad
     * module would otherwise take the whole client down — and a module that
     * worked on one Minecraft version can meet a changed method on the next
     * (a LinkageError, hence that in the catch too). The module is disabled
     * and that's persisted, so it stays off across a restart until the
     * player turns it back on.
     */
    private void quarantine(AxoModule module, String where, Throwable error) {
        AxoClient.LOGGER.error("Module '{}' failed during {} and was turned off", module.id(), where, error);
        enabled.put(module.id(), false);
        config.setEnabled(module.id(), false);
        try {
            module.onDisable();
        } catch (Exception | LinkageError e) {
            AxoClient.LOGGER.error("Module '{}' also failed while turning off", module.id(), e);
        }
        dev.axoclient.gui.notify.Notifications.push(
            module.displayName() + " hit an error and was turned off",
            dev.axoclient.gui.notify.Notification.Type.WARN
        );
    }

    /** Dispatched from {@link dev.axoclient.mixin.GuiMixin} after the vanilla HUD. */
    public void renderHud(GuiGraphics graphics) {
        if (Minecraft.getInstance().options.hideGui) {
            return;
        }
        for (AxoModule module : modules.values()) {
            if (module instanceof HudRenderable hud && isEnabled(module)) {
                try {
                    hud.renderHud(graphics);
                } catch (Exception | LinkageError e) {
                    quarantine(module, "HUD render", e);
                }
            }
        }
    }
}
