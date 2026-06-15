package cc.fascinated.monitor.service.agent;

import cc.fascinated.monitor.exception.impl.BadRequestException;
import cc.fascinated.monitor.util.Semver;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AgentReleaseSupport {

    public static Optional<GitHubRelease> selectLatestRelease(List<GitHubRelease> releases, String tagPrefix) {
        GitHubRelease best = null;
        Semver.Version bestVersion = null;

        for (GitHubRelease release : releases) {
            if (!isEligibleRelease(release, tagPrefix)) {
                continue;
            }
            Semver.Version version;
            try {
                version = Semver.parse(stripTagPrefix(release.tagName(), tagPrefix));
            } catch (BadRequestException ex) {
                continue;
            }
            if (best == null
                    || version.isGreaterThan(bestVersion)
                    || (version.compareTo(bestVersion) == 0 && releaseIsNewer(release, best))) {
                best = release;
                bestVersion = version;
            }
        }
        return Optional.ofNullable(best);
    }

    public static Map<String, String> parseChecksums(String content) {
        Map<String, String> checksums = new HashMap<>();
        for (String line : content.trim().split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] fields = trimmed.split("\\s+");
            if (fields.length != 2) {
                continue;
            }
            checksums.put(fields[1], fields[0]);
        }
        return checksums;
    }

    public static boolean isAgentBinary(String name) {
        return name != null && name.startsWith("monitor-agent-") && !name.endsWith(".txt");
    }

    public static String stripTagPrefix(String tagName, String tagPrefix) {
        if (tagName.startsWith(tagPrefix)) {
            return tagName.substring(tagPrefix.length());
        }
        throw new BadRequestException("invalid release tag");
    }

    private static boolean isEligibleRelease(GitHubRelease release, String tagPrefix) {
        return !release.draft()
                && !release.prerelease()
                && release.tagName() != null
                && release.tagName().startsWith(tagPrefix);
    }

    private static boolean releaseIsNewer(GitHubRelease candidate, GitHubRelease current) {
        if (candidate.publishedAt() == null) {
            return false;
        }
        if (current.publishedAt() == null) {
            return true;
        }
        return candidate.publishedAt().isAfter(current.publishedAt());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubRelease(
            @JsonProperty("tag_name") String tagName,
            boolean draft,
            boolean prerelease,
            @JsonProperty("published_at") Instant publishedAt,
            List<GitHubAsset> assets
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubAsset(
            String name,
            @JsonProperty("browser_download_url") String browserDownloadUrl
    ) {}
}
