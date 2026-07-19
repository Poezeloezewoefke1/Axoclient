package dev.axoclient.hud;

import java.util.Locale;

/**
 * Nine screen anchor points (roadmap P1-06). Offsets are added after
 * anchoring, so negative offsets pull toward the screen edge on
 * right/bottom anchors.
 */
public enum HudAnchor {
    TOP_LEFT(0.0f, 0.0f),
    TOP_CENTER(0.5f, 0.0f),
    TOP_RIGHT(1.0f, 0.0f),
    MIDDLE_LEFT(0.0f, 0.5f),
    CENTER(0.5f, 0.5f),
    MIDDLE_RIGHT(1.0f, 0.5f),
    BOTTOM_LEFT(0.0f, 1.0f),
    BOTTOM_CENTER(0.5f, 1.0f),
    BOTTOM_RIGHT(1.0f, 1.0f);

    private final float horizontal;
    private final float vertical;

    HudAnchor(float horizontal, float vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public int x(int screenWidth, int elementWidth, int offsetX) {
        return Math.round((screenWidth - elementWidth) * horizontal) + offsetX;
    }

    public int y(int screenHeight, int elementHeight, int offsetY) {
        return Math.round((screenHeight - elementHeight) * vertical) + offsetY;
    }

    /** Lenient config parsing: unknown names fall back instead of crashing. */
    public static HudAnchor fromId(String id, HudAnchor fallback) {
        if (id == null) {
            return fallback;
        }
        try {
            return valueOf(id.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
