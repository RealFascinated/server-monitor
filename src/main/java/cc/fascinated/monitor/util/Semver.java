package cc.fascinated.monitor.util;

import cc.fascinated.monitor.exception.impl.BadRequestException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Semver {
    public static Version parse(String raw) {
        if (raw == null) {
            throw new BadRequestException("version is required");
        }

        String trimmed = raw.trim();
        if (trimmed.startsWith("v")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.isEmpty()) {
            throw new BadRequestException("version is required");
        }

        String core = trimmed;
        String prerelease = "";
        int dash = trimmed.indexOf('-');
        if (dash >= 0) {
            core = trimmed.substring(0, dash);
            prerelease = trimmed.substring(dash + 1);
        }

        String[] parts = core.split("\\.");
        if (parts.length < 1 || parts.length > 3) {
            throw new BadRequestException("invalid version");
        }

        try {
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return new Version(major, minor, patch, prerelease);
        } catch (NumberFormatException ex) {
            throw new BadRequestException("invalid version");
        }
    }

    public record Version(int major, int minor, int patch, String prerelease) implements Comparable<Version> {
        @Override
        public int compareTo(Version other) {
            int majorCompare = Integer.compare(this.major, other.major);
            if (majorCompare != 0) {
                return majorCompare;
            }
            int minorCompare = Integer.compare(this.minor, other.minor);
            if (minorCompare != 0) {
                return minorCompare;
            }
            int patchCompare = Integer.compare(this.patch, other.patch);
            if (patchCompare != 0) {
                return patchCompare;
            }
            if (this.prerelease.isEmpty() && other.prerelease.isEmpty()) {
                return 0;
            }
            if (this.prerelease.isEmpty()) {
                return 1;
            }
            if (other.prerelease.isEmpty()) {
                return -1;
            }
            return this.prerelease.compareTo(other.prerelease);
        }

        public boolean isGreaterThan(Version other) {
            return this.compareTo(other) > 0;
        }

        @Override
        public String toString() {
            return this.major + "." + this.minor + "." + this.patch;
        }
    }
}
