package cc.fascinated.monitor.service;

import cc.fascinated.monitor.config.AgentUpdateProperties;
import cc.fascinated.monitor.exception.impl.InternalServerException;
import cc.fascinated.monitor.model.dto.response.agent.AgentUpdateAssetResponse;
import cc.fascinated.monitor.model.dto.response.agent.AgentUpdateResponse;
import cc.fascinated.monitor.service.agent.AgentReleaseSupport;
import cc.fascinated.monitor.service.agent.AgentReleaseSupport.GitHubAsset;
import cc.fascinated.monitor.service.agent.AgentReleaseSupport.GitHubRelease;
import cc.fascinated.monitor.util.Constants;
import cc.fascinated.monitor.util.Semver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class AgentUpdateService {
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);

    private final AgentUpdateProperties properties;
    private final HttpClient httpClient;

    private volatile CachedRelease cachedRelease;

    public AgentUpdateService(AgentUpdateProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(HTTP_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public Optional<AgentUpdateResponse> checkUpdate(String version) {
        Semver.Version current = Semver.parse(version);
        CachedRelease latest = this.fetchLatestRelease();
        Semver.Version latestVersion = Semver.parse(
                AgentReleaseSupport.stripTagPrefix(latest.release().tagName(), this.properties.getTagPrefix())
        );
        if (!latestVersion.isGreaterThan(current)) {
            return Optional.empty();
        }

        List<AgentUpdateAssetResponse> assets = new ArrayList<>();
        for (GitHubAsset asset : latest.release().assets()) {
            if (!AgentReleaseSupport.isAgentBinary(asset.name())) {
                continue;
            }
            String checksum = latest.checksumsByAsset().get(asset.name());
            if (checksum == null || checksum.isBlank()) {
                continue;
            }
            assets.add(new AgentUpdateAssetResponse(asset.name(), asset.browserDownloadUrl(), checksum));
        }
        if (assets.isEmpty()) {
            throw new InternalServerException("Latest agent release has no downloadable assets");
        }
        return Optional.of(new AgentUpdateResponse(latestVersion.toString(), assets));
    }

    private CachedRelease fetchLatestRelease() {
        CachedRelease cached = this.cachedRelease;
        Instant now = Instant.now();
        if (cached != null && cached.fetchedAt().plus(this.properties.getReleaseCacheTtl()).isAfter(now)) {
            return cached;
        }

        synchronized (this) {
            cached = this.cachedRelease;
            if (cached != null && cached.fetchedAt().plus(this.properties.getReleaseCacheTtl()).isAfter(now)) {
                return cached;
            }

            List<GitHubRelease> releases = this.fetchReleases();
            GitHubRelease latest = AgentReleaseSupport.selectLatestRelease(releases, this.properties.getTagPrefix())
                    .orElseThrow(() -> new InternalServerException("No agent releases found"));
            Map<String, String> checksums = this.fetchChecksums(latest);
            cached = new CachedRelease(now, latest, checksums);
            this.cachedRelease = cached;
            return cached;
        }
    }

    private List<GitHubRelease> fetchReleases() {
        String url = "https://api.github.com/repos/%s/%s/releases?per_page=100"
                .formatted(this.properties.getGithubOwner(), this.properties.getGithubRepo());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "monitor-api-agent-updater")
                .GET()
                .build();
        try {
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new InternalServerException("Failed to fetch agent releases (" + response.statusCode() + ")");
            }
            GitHubRelease[] releases = Constants.OBJECT_MAPPER.readValue(response.body(), GitHubRelease[].class);
            return List.of(releases);
        } catch (InternalServerException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to fetch agent releases: {}", ex.getMessage());
            throw new InternalServerException("Failed to fetch agent releases");
        }
    }

    private Map<String, String> fetchChecksums(GitHubRelease release) {
        GitHubAsset checksumsAsset = null;
        for (GitHubAsset asset : release.assets()) {
            if ("checksums.txt".equals(asset.name())) {
                checksumsAsset = asset;
                break;
            }
        }
        if (checksumsAsset == null) {
            throw new InternalServerException("Latest agent release is missing checksums.txt");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(checksumsAsset.browserDownloadUrl()))
                .timeout(HTTP_TIMEOUT)
                .header("User-Agent", "monitor-api-agent-updater")
                .GET()
                .build();
        try {
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new InternalServerException("Failed to fetch agent checksums (" + response.statusCode() + ")");
            }
            return AgentReleaseSupport.parseChecksums(response.body());
        } catch (InternalServerException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to fetch agent checksums: {}", ex.getMessage());
            throw new InternalServerException("Failed to fetch agent checksums");
        }
    }

    private record CachedRelease(Instant fetchedAt, GitHubRelease release, Map<String, String> checksumsByAsset) {}
}
