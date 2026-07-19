package dev.axoclient.hud;

import dev.axoclient.core.AxoConfig;

/**
 * A module's HUD placement: anchor + pixel offsets, persisted in the
 * module's config section as hud_anchor / hud_x / hud_y.
 */
public record HudPosition(HudAnchor anchor, int offsetX, int offsetY) {

    public static HudPosition load(AxoConfig config, String moduleId, HudPosition fallback) {
        return new HudPosition(
            HudAnchor.fromId(
                config.getModuleString(moduleId, "hud_anchor", fallback.anchor().name()),
                fallback.anchor()
            ),
            config.getModuleInt(moduleId, "hud_x", fallback.offsetX()),
            config.getModuleInt(moduleId, "hud_y", fallback.offsetY())
        );
    }
}
