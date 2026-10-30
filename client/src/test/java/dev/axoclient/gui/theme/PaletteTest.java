package dev.axoclient.gui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PaletteTest {

    @Test
    void fifteenThemesTwelveDarkThreeLight() {
        assertEquals(15, Palette.ALL.size());
        assertEquals(3, Palette.ALL.stream().filter(Palette::light).count());
    }

    @Test
    void idsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (Palette palette : Palette.ALL) {
            assertTrue(ids.add(palette.id()), palette.id());
        }
    }

    @Test
    void coloursAreOpaqueAndMatchTheDesign() {
        Palette midnight = Palette.byId("midnight");
        assertEquals(0xFF8B7CF6, midnight.accent());
        assertEquals(0xFF0A0C18, midnight.panel());
        for (Palette palette : Palette.ALL) {
            assertEquals(0xFF, palette.text() >>> 24, palette.id());
        }
    }

    @Test
    void unknownIdFallsBackToMidnight() {
        assertEquals("midnight", Palette.byId("nope").id());
        assertEquals("midnight", Palette.byId(null).id());
    }

    @Test
    void softAccentKeepsColourAndDropsAlpha() {
        Palette ocean = Palette.byId("ocean");
        assertEquals(0x005B9BFF, ocean.accentSoft() & 0x00FFFFFF);
        assertEquals(0x2E, ocean.accentSoft() >>> 24);
        assertEquals(0x24, Palette.byId("snow").accentSoft() >>> 24);
    }
}
