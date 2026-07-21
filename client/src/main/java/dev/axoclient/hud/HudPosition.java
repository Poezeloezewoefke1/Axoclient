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

    /** Persists this placement into the module's config section. */
    public void save(AxoConfig config, String moduleId) {
        config.setModuleString(moduleId, "hud_anchor", anchor.name());
        config.setModuleInt(moduleId, "hud_x", offsetX);
        config.setModuleInt(moduleId, "hud_y", offsetY);
    }

    /**
     * Converts an absolute top-left screen position into the placement whose
     * anchor keeps the offsets smallest — i.e. magnetic snapping to the
     * nearest of the nine anchor points. Used by the HUD editor on drag.
     */
    public static HudPosition fromAbsolute(int x, int y, int elementW, int elementH, int screenW, int screenH) {
        HudAnchor best = HudAnchor.TOP_LEFT;
        long bestCost = Long.MAX_VALUE;
        int bestOffX = x;
        int bestOffY = y;
        for (HudAnchor anchor : HudAnchor.values()) {
            int offX = x - anchor.x(screenW, elementW, 0);
            int offY = y - anchor.y(screenH, elementH, 0);
            long cost = (long) offX * offX + (long) offY * offY;
            if (cost < bestCost) {
                bestCost = cost;
                best = anchor;
                bestOffX = offX;
                bestOffY = offY;
            }
        }
        return new HudPosition(best, bestOffX, bestOffY);
    }
}
