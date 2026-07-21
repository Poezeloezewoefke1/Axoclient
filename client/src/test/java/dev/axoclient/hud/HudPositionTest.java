package dev.axoclient.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * {@link HudPosition#fromAbsolute} is the HUD editor's snap logic. The key
 * invariant is that snapping re-expresses a position without ever moving it:
 * re-applying the chosen anchor + offset must reproduce the exact input
 * pixel. Corner/centre cases additionally assert the expected magnetic anchor.
 */
class HudPositionTest {

    private static final int W = 200;
    private static final int H = 100;
    private static final int EW = 20;
    private static final int EH = 10;

    @Test
    void snapsExactCornersToCornerAnchorsWithZeroOffset() {
        HudPosition tl = HudPosition.fromAbsolute(0, 0, EW, EH, W, H);
        assertEquals(HudAnchor.TOP_LEFT, tl.anchor());
        assertEquals(0, tl.offsetX());
        assertEquals(0, tl.offsetY());

        HudPosition br = HudPosition.fromAbsolute(W - EW, H - EH, EW, EH, W, H);
        assertEquals(HudAnchor.BOTTOM_RIGHT, br.anchor());
        assertEquals(0, br.offsetX());
        assertEquals(0, br.offsetY());
    }

    @Test
    void snapsScreenCentreToCenterAnchor() {
        int cx = Math.round((W - EW) * 0.5f);
        int cy = Math.round((H - EH) * 0.5f);
        HudPosition p = HudPosition.fromAbsolute(cx, cy, EW, EH, W, H);
        assertEquals(HudAnchor.CENTER, p.anchor());
    }

    @Test
    void snappingNeverMovesTheElement() {
        int[][] points = {
            { 0, 0 }, { 5, 3 }, { 90, 45 }, { 150, 12 }, { 180, 90 }, { 33, 77 }, { 199, 0 }
        };
        for (int[] pt : points) {
            HudPosition p = HudPosition.fromAbsolute(pt[0], pt[1], EW, EH, W, H);
            int backX = p.anchor().x(W, EW, p.offsetX());
            int backY = p.anchor().y(H, EH, p.offsetY());
            assertEquals(pt[0], backX, "x preserved for " + pt[0] + "," + pt[1]);
            assertEquals(pt[1], backY, "y preserved for " + pt[0] + "," + pt[1]);
        }
    }
}
