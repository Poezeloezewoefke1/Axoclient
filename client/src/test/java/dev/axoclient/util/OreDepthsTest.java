package dev.axoclient.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class OreDepthsTest {

    @Test
    void deepslateIsDiamondsAndRedstone() {
        assertEquals(List.of("diamonds", "redstone"), OreDepths.bestAt(WorldInfo.OVERWORLD, -59));
    }

    @Test
    void ironPeakAtSixteen() {
        assertEquals(List.of("iron"), OreDepths.bestAt(WorldInfo.OVERWORLD, 16));
    }

    @Test
    void mountainTopsDoNotRepeatIron() {
        assertEquals(List.of("iron", "emeralds (mountains)"), OreDepths.bestAt(WorldInfo.OVERWORLD, 232));
    }

    @Test
    void netherDebrisBand() {
        assertEquals(List.of("ancient debris"), OreDepths.bestAt(WorldInfo.NETHER, 15));
    }

    @Test
    void nothingBetweenBandsOrInTheEnd() {
        assertTrue(OreDepths.bestAt(WorldInfo.OVERWORLD, 70).isEmpty());
        assertTrue(OreDepths.bestAt(WorldInfo.END, 15).isEmpty());
        assertTrue(OreDepths.bestAt(null, 15).isEmpty());
    }
}
