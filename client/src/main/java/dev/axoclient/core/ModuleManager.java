package dev.axoclient.core;

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
        if (state) {
            module.onEnable();
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
        if (state) {
            module.onEnable();
        } else {
            module.onDisable();
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
                module.onTick();
            }
        }
    }

    /** Dispatched from {@link dev.axoclient.mixin.GuiMixin} after the vanilla HUD. */
    public void renderHud(GuiGraphics graphics) {
        if (Minecraft.getInstance().options.hideGui) {
            return;
        }
        for (AxoModule module : modules.values()) {
            if (module instanceof HudRenderable hud && isEnabled(module)) {
                hud.renderHud(graphics);
            }
        }
    }
}
