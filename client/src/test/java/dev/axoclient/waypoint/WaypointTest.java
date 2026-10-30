package dev.axoclient.waypoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Waypoint storage and bearing maths have no Minecraft dependency. */
class WaypointTest {

    @Test
    void encodeDecodeRoundTrips() {
        List<Waypoint> points = List.of(
            new Waypoint("Home", "play.example.net", "overworld", 100, 64, -200),
            new Waypoint("Portal", "sp:My World", "nether", -12, 70, 8)
        );
        assertEquals(points, Waypoint.decode(Waypoint.encode(points)));
    }

    @Test
    void separatorsInNamesAreStripped() {
        List<Waypoint> decoded = Waypoint.decode(Waypoint.encode(List.of(
            new Waypoint("a\tb\nc", "w", "overworld", 1, 2, 3)
        )));
        assertEquals(1, decoded.size());
        assertEquals("a b c", decoded.get(0).name());
    }

    @Test
    void malformedEntriesAreSkipped() {
        String encoded = "Good\tw\toverworld\t1\t2\t3\nbroken\nBad\tw\toverworld\tx\t2\t3";
        List<Waypoint> decoded = Waypoint.decode(encoded);
        assertEquals(1, decoded.size());
        assertEquals("Good", decoded.get(0).name());
        assertTrue(Waypoint.decode(null).isEmpty());
        assertTrue(Waypoint.decode("").isEmpty());
    }

    @Test
    void straightAheadIsUpArrow() {
        // Yaw 0 faces south (+Z); a point due south is dead ahead.
        Waypoint south = new Waypoint("s", "w", "overworld", 0, 64, 100);
        float turn = south.turnFrom(0.5, 0.5, 0.0F);
        assertEquals(0.0F, turn, 0.01F);
        assertEquals("↑", Waypoint.arrowFor(turn));
    }

    @Test
    void westIsToTheRightWhenFacingSouth() {
        // Turning right from south faces west (yaw 90).
        Waypoint west = new Waypoint("w", "w", "overworld", -100, 64, 0);
        float turn = west.turnFrom(0.5, 0.5, 0.0F);
        assertEquals(90.0F, turn, 0.01F);
        assertEquals("→", Waypoint.arrowFor(turn));
    }

    @Test
    void behindIsDownArrowAndLeftIsLeftArrow() {
        assertEquals("↓", Waypoint.arrowFor(180.0F));
        assertEquals("↓", Waypoint.arrowFor(-180.0F));
        assertEquals("←", Waypoint.arrowFor(-90.0F));
        assertEquals("↖", Waypoint.arrowFor(-40.0F));
    }

    @Test
    void wrapKeepsTurnsShort() {
        assertEquals(-10.0F, Waypoint.wrapDegrees(350.0F), 0.001F);
        assertEquals(10.0F, Waypoint.wrapDegrees(-350.0F), 0.001F);
    }

    @Test
    void distanceFormatting() {
        assertEquals("42m", Waypoint.formatDistance(42.4));
        assertEquals("1.5km", Waypoint.formatDistance(1500));
    }
}
