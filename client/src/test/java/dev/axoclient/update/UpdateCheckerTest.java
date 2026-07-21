package dev.axoclient.update;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Version comparison is pure integer math — no Minecraft needed to test it. */
class UpdateCheckerTest {

    @Test
    void detectsNewerVersions() {
        assertTrue(UpdateChecker.isNewer("0.2.0", "0.1.0"));
        assertTrue(UpdateChecker.isNewer("0.1.1", "0.1.0"));
        assertTrue(UpdateChecker.isNewer("1.0.0", "0.9.9"));
    }

    @Test
    void treatsEqualAndOlderAsNotNewer() {
        assertFalse(UpdateChecker.isNewer("0.1.0", "0.1.0"));
        assertFalse(UpdateChecker.isNewer("0.1.0", "0.2.0"));
        assertFalse(UpdateChecker.isNewer("0.9.9", "1.0.0"));
    }

    @Test
    void comparesNumericallyNotLexically() {
        // 10 > 9 as integers, even though "10" < "9" as strings.
        assertTrue(UpdateChecker.isNewer("0.10.0", "0.9.0"));
    }

    @Test
    void ignoresBuildAndPreReleaseSuffixesAndPads() {
        assertFalse(UpdateChecker.isNewer("0.2.0+build.7", "0.2.0"));
        assertFalse(UpdateChecker.isNewer("1.2", "1.2.0"));
        assertTrue(UpdateChecker.isNewer("1.2.1", "1.2"));
    }
}
