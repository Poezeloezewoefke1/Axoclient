package dev.axoclient.waypoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A saved spot in a world. Pure data plus the maths the HUD needs, with no
 * Minecraft types, so it unit-tests without booting the game.
 *
 * {@code world} identifies the server or singleplayer save and {@code dim}
 * the dimension, so a "Home" in one world never points you across another.
 */
public record Waypoint(String name, String world, String dim, int x, int y, int z) {
    private static final String ENTRY_SEP = "\n";
    private static final String FIELD_SEP = "\t";

    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    public boolean isIn(String currentWorld, String currentDim) {
        return world.equals(currentWorld) && dim.equals(currentDim);
    }

    public double horizontalDistance(double fromX, double fromZ) {
        double dx = x + 0.5 - fromX;
        double dz = z + 0.5 - fromZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Degrees to turn from the player's yaw to face this waypoint, in
     * -180..180. Positive means turn right. Minecraft yaw 0 faces south (+Z)
     * and increases clockwise seen from above.
     */
    public float turnFrom(double fromX, double fromZ, float playerYaw) {
        double dx = x + 0.5 - fromX;
        double dz = z + 0.5 - fromZ;
        float bearing = (float) Math.toDegrees(Math.atan2(-dx, dz));
        return wrapDegrees(bearing - playerYaw);
    }

    /** One of eight arrows for a turn from {@link #turnFrom}; up means straight ahead. */
    public static String arrowFor(float turn) {
        int index = Math.floorMod(Math.round(turn / 45.0F), ARROWS.length);
        return ARROWS[index];
    }

    static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    /**
     * Serialises waypoints into one config string. Tabs and newlines are the
     * separators because neither can appear in a server address or a name
     * typed in chat; they are stripped from names defensively anyway.
     */
    public static String encode(List<Waypoint> points) {
        StringBuilder out = new StringBuilder();
        for (Waypoint point : points) {
            if (out.length() > 0) {
                out.append(ENTRY_SEP);
            }
            out.append(clean(point.name())).append(FIELD_SEP)
                .append(clean(point.world())).append(FIELD_SEP)
                .append(clean(point.dim())).append(FIELD_SEP)
                .append(point.x()).append(FIELD_SEP)
                .append(point.y()).append(FIELD_SEP)
                .append(point.z());
        }
        return out.toString();
    }

    /** Inverse of {@link #encode}; malformed entries are skipped, never fatal. */
    public static List<Waypoint> decode(String encoded) {
        List<Waypoint> points = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) {
            return points;
        }
        for (String entry : encoded.split(ENTRY_SEP)) {
            String[] fields = entry.split(FIELD_SEP, -1);
            if (fields.length != 6) {
                continue;
            }
            try {
                points.add(new Waypoint(
                    fields[0],
                    fields[1],
                    fields[2],
                    Integer.parseInt(fields[3].trim()),
                    Integer.parseInt(fields[4].trim()),
                    Integer.parseInt(fields[5].trim())
                ));
            } catch (NumberFormatException ignored) {
                // One bad line shouldn't cost the player every other waypoint.
            }
        }
        return points;
    }

    /** "1.2km" past a kilometre, otherwise whole metres. */
    public static String formatDistance(double blocks) {
        if (blocks >= 1000) {
            return String.format(Locale.ROOT, "%.1fkm", blocks / 1000.0);
        }
        return Math.round(blocks) + "m";
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace(FIELD_SEP, " ").replace(ENTRY_SEP, " ");
    }
}
