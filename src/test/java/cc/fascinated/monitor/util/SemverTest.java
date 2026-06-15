package cc.fascinated.monitor.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemverTest {
    @Test
    void comparesVersions() {
        Semver.Version current = Semver.parse("2.0.0");
        Semver.Version patch = Semver.parse("2.0.1");
        Semver.Version minor = Semver.parse("2.1.0");

        assertTrue(patch.isGreaterThan(current));
        assertTrue(minor.isGreaterThan(current));
        assertFalse(current.isGreaterThan(patch));
    }

    @Test
    void acceptsVPrefixedVersions() {
        Semver.Version version = Semver.parse("v2.0.10");
        assertTrue(version.isGreaterThan(Semver.parse("2.0.9")));
    }
}
