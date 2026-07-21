package dev.axoclient.update;

/**
 * Dotted-version comparison for the in-game update check. Deliberately has no
 * Minecraft or Fabric dependency so it unit-tests cleanly (see the test
 * source set). Build/pre-release suffixes after {@code -} or {@code +} are
 * ignored; missing trailing components compare as zero (so {@code 1.2} equals
 * {@code 1.2.0}).
 */
public final class Versions {
    private Versions() {
    }

    /** True when {@code candidate} is a strictly higher version than {@code current}. */
    public static boolean isNewer(String candidate, String current) {
        int[] a = parse(candidate);
        int[] b = parse(current);
        int len = Math.max(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int av = i < a.length ? a[i] : 0;
            int bv = i < b.length ? b[i] : 0;
            if (av != bv) {
                return av > bv;
            }
        }
        return false;
    }

    private static int[] parse(String version) {
        if (version == null) {
            return new int[0];
        }
        String core = version.split("[-+]", 2)[0];
        String[] parts = core.split("\\.");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                out[i] = 0;
            }
        }
        return out;
    }
}
