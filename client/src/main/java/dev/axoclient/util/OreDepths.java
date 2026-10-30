package dev.axoclient.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Which ores are at their most common at a given height, for the Depth Guide.
 * Bands follow the 1.18+ ore distribution: each is the stretch around an
 * ore's peak where branch-mining for it actually pays, not the full range it
 * can generate in. Pure data, so it is unit-tested without the game.
 */
public final class OreDepths {
    private record Band(String ore, int minY, int maxY) {
        boolean contains(int y) {
            return y >= minY && y <= maxY;
        }
    }

    private static final Band[] OVERWORLD = {
        new Band("diamonds", -64, -50),
        new Band("redstone", -64, -50),
        new Band("gold", -26, -6),
        new Band("lapis", -10, 10),
        new Band("iron", 6, 26),
        new Band("copper", 38, 58),
        new Band("coal", 86, 106),
        new Band("iron", 200, 320),
        new Band("emeralds (mountains)", 200, 320)
    };

    private static final Band[] NETHER = {
        new Band("ancient debris", 8, 22),
        new Band("quartz & nether gold", 23, 117)
    };

    private OreDepths() {
    }

    /** Ores at their best around {@code y}, in table order without repeats; empty when none. */
    public static List<String> bestAt(String dimension, int y) {
        Band[] bands = WorldInfo.NETHER.equals(dimension) ? NETHER
            : WorldInfo.OVERWORLD.equals(dimension) ? OVERWORLD
            : new Band[0];
        List<String> ores = new ArrayList<>();
        for (Band band : bands) {
            if (band.contains(y) && !ores.contains(band.ore())) {
                ores.add(band.ore());
            }
        }
        return ores;
    }
}
