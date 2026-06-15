package cc.fascinated.monitor.service.agent;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentReleaseSupportTest {
    @Test
    void selectsLatestEligibleRelease() {
        List<AgentReleaseSupport.GitHubRelease> releases = List.of(
                release("agent/v2.0.5", "2026-01-01T00:00:00Z"),
                release("agent/v2.0.10", "2026-01-02T00:00:00Z"),
                release("agent/v2.0.7", "2026-05-01T00:00:00Z"),
                release("v1.0.0", "2026-06-01T00:00:00Z"),
                release("agent/v2.1.0", "2026-06-02T00:00:00Z", true, false),
                release("agent/v2.0.0-beta", "2026-06-03T00:00:00Z", false, true)
        );

        AgentReleaseSupport.GitHubRelease latest = AgentReleaseSupport
                .selectLatestRelease(releases, "agent/v")
                .orElseThrow();
        assertEquals("agent/v2.0.10", latest.tagName());
    }

    @Test
    void tieBreaksByPublishDate() {
        Instant older = Instant.parse("2026-01-01T00:00:00Z");
        Instant newer = Instant.parse("2026-02-01T00:00:00Z");
        List<AgentReleaseSupport.GitHubRelease> releases = List.of(
                new AgentReleaseSupport.GitHubRelease("agent/v2.0.7", false, false, older, List.of()),
                new AgentReleaseSupport.GitHubRelease("agent/v2.0.7", false, false, newer, List.of())
        );

        AgentReleaseSupport.GitHubRelease latest = AgentReleaseSupport
                .selectLatestRelease(releases, "agent/v")
                .orElseThrow();
        assertEquals(newer, latest.publishedAt());
    }

    @Test
    void parsesChecksums() {
        Map<String, String> checksums = AgentReleaseSupport.parseChecksums(
                "abc123  monitor-agent-linux-amd64\n" +
                        "def456  checksums.txt\n"
        );

        assertEquals("abc123", checksums.get("monitor-agent-linux-amd64"));
    }

    private static AgentReleaseSupport.GitHubRelease release(String tag, String publishedAt) {
        return release(tag, publishedAt, false, false);
    }

    private static AgentReleaseSupport.GitHubRelease release(
            String tag,
            String publishedAt,
            boolean draft,
            boolean prerelease
    ) {
        return new AgentReleaseSupport.GitHubRelease(
                tag,
                draft,
                prerelease,
                Instant.parse(publishedAt),
                List.of()
        );
    }
}
