package dev.axoclient.update;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Version comparison drives whether the in-game updater prompts a download,
 * so its edges (unequal lengths, suffixes, equality) are locked here. No
 * Minecraft dependency, so it runs in the plain JUnit source set.
 */
class VersionsTest {

    @Test
    void detectsHigherVersions() {
        assertTrue(Versions.isNewer("0.2.0", "0.1.0"));
        assertTrue(Versions.isNewer("1.0.0", "0.9.9"));
        assertTrue(Versions.isNewer("0.1.1", "0.1.0"));
        assertTrue(Versions.isNewer("1.2.10", "1.2.9"));
    }

    @Test
    void equalOrOlderIsNotNewer() {
        assertFalse(Versions.isNewer("0.1.0", "0.1.0"));
        assertFalse(Versions.isNewer("0.1.0", "0.2.0"));
        assertFalse(Versions.isNewer("1.2.9", "1.2.10"));
    }

    @Test
    void missingTrailingComponentsCompareAsZero() {
        assertFalse(Versions.isNewer("1.2", "1.2.0"));
        assertFalse(Versions.isNewer("1.2.0", "1.2"));
        assertTrue(Versions.isNewer("1.2.1", "1.2"));
    }

    @Test
    void ignoresBuildAndPreReleaseSuffixes() {
        assertFalse(Versions.isNewer("0.1.0+build5", "0.1.0"));
        assertTrue(Versions.isNewer("0.2.0-rc1", "0.1.0"));
        assertFalse(Versions.isNewer("0.1.0", "0.1.0-beta"));
    }

    @Test
    void handlesNonNumericAndNullGracefully() {
        assertFalse(Versions.isNewer("abc", "0.0.0"));
        assertFalse(Versions.isNewer(null, "0.1.0"));
        assertTrue(Versions.isNewer("0.1.0", null));
    }
}
