package dev.axoclient.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Anchor math has no Minecraft dependency, so it unit-tests cleanly. */
class HudAnchorTest {

    @Test
    void topLeftAddsRawOffset() {
        assertEquals(4, HudAnchor.TOP_LEFT.x(100, 10, 4));
        assertEquals(4, HudAnchor.TOP_LEFT.y(100, 10, 4));
    }

    @Test
    void bottomRightPullsInWithNegativeOffset() {
        // (100 - 10) * 1.0 = 90, then - 4.
        assertEquals(86, HudAnchor.BOTTOM_RIGHT.x(100, 10, -4));
        assertEquals(86, HudAnchor.BOTTOM_RIGHT.y(100, 10, -4));
    }

    @Test
    void centerHalves() {
        assertEquals(45, HudAnchor.CENTER.x(100, 10, 0));
        assertEquals(45, HudAnchor.CENTER.y(100, 10, 0));
    }

    @Test
    void fromIdIsCaseInsensitiveAndLenient() {
        assertEquals(HudAnchor.TOP_LEFT, HudAnchor.fromId("top_left", HudAnchor.CENTER));
        assertEquals(HudAnchor.BOTTOM_RIGHT, HudAnchor.fromId("  BOTTOM_RIGHT ", HudAnchor.CENTER));
        assertEquals(HudAnchor.CENTER, HudAnchor.fromId("not-an-anchor", HudAnchor.CENTER));
        assertEquals(HudAnchor.CENTER, HudAnchor.fromId(null, HudAnchor.CENTER));
    }
}
