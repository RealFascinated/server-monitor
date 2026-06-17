package cc.fascinated.monitor.service;

import cc.fascinated.monitor.config.MonitorMaxMindProperties;
import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Subdivision;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@Slf4j
@Service
public class MaxMindService {
    private static final String CITY_EDITION = "GeoLite2-City";
    private static final String DATABASE_DOWNLOAD_ENDPOINT =
            "https://download.maxmind.com/app/geoip_download?edition_id=%s&license_key=%s&suffix=tar.gz";
    private static final long UPDATE_AFTER_MILLIS = 3L * 24L * 60L * 60L * 1000L;

    private final MonitorMaxMindProperties properties;
    private DatabaseReader cityReader;

    public MaxMindService(MonitorMaxMindProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() {
        if (this.properties.getLicense().isBlank()) {
            log.info("MaxMind license not configured; GeoIP lookups disabled");
            return;
        }
        loadDatabase(false);
    }

    @PreDestroy
    @SneakyThrows
    public void cleanup() {
        if (this.cityReader != null) {
            this.cityReader.close();
            this.cityReader = null;
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledDatabaseUpdate() {
        if (this.properties.getLicense().isBlank()) {
            return;
        }
        loadDatabase(true);
    }

    public Optional<String> formatLocation(String ip) {
        if (this.cityReader == null || ip == null || ip.isBlank()) {
            return Optional.empty();
        }

        try {
            CityResponse city = this.cityReader.city(InetAddress.getByName(ip));
            Country country = city.getCountry();
            String countryName = country != null ? country.getName() : null;
            com.maxmind.geoip2.record.City cityRecord = city.getCity();
            String cityName = cityRecord != null ? cityRecord.getName() : null;
            Subdivision subdivision = city.getMostSpecificSubdivision();
            String regionName = subdivision != null ? subdivision.getName() : null;

            if (cityName != null && countryName != null) {
                return Optional.of(cityName + ", " + countryName);
            }
            if (regionName != null && countryName != null) {
                return Optional.of(regionName + ", " + countryName);
            }
            if (countryName != null) {
                return Optional.of(countryName);
            }
            return Optional.empty();
        } catch (GeoIp2Exception ex) {
            log.debug("GeoIP lookup failed for {}: {}", ip, ex.getMessage());
            return Optional.empty();
        } catch (IOException ex) {
            log.debug("GeoIP lookup failed for {}: {}", ip, ex.getMessage());
            return Optional.empty();
        }
    }

    @SneakyThrows
    private void loadDatabase(boolean scheduled) {
        File databaseDir = new File(this.properties.getDatabaseDir());
        if (!databaseDir.exists()) {
            databaseDir.mkdirs();
        }

        File databaseFile = new File(databaseDir, CITY_EDITION + ".mmdb");
        boolean fileExisted = databaseFile.exists();
        boolean needsUpdate = false;

        if (fileExisted && scheduled) {
            long ageInMillis = System.currentTimeMillis() - databaseFile.lastModified();
            if (ageInMillis > UPDATE_AFTER_MILLIS) {
                needsUpdate = true;
                log.info("MaxMind database is {} days old, attempting update...", ageInMillis / UPDATE_AFTER_MILLIS);
            }
        } else if (!fileExisted) {
            log.info("MaxMind database not found, downloading...");
        }

        if (needsUpdate) {
            closeReader();
        }

        if (!databaseFile.exists() || needsUpdate) {
            boolean downloaded = downloadDatabase(databaseFile, databaseDir);
            if (!downloaded) {
                if (fileExisted && needsUpdate) {
                    log.warn("MaxMind download failed; keeping existing database");
                } else {
                    log.warn("MaxMind download failed; GeoIP lookups will be unavailable");
                    return;
                }
            }
        }

        if (!databaseFile.exists()) {
            return;
        }

        closeReader();
        this.cityReader = new DatabaseReader.Builder(databaseFile).withCache(new CHMCache()).build();
        log.info("Loaded MaxMind database from {}", databaseFile.getAbsolutePath());
    }

    private void closeReader() throws IOException {
        if (this.cityReader != null) {
            this.cityReader.close();
            this.cityReader = null;
        }
    }

    private boolean downloadDatabase(File databaseFile, File databaseDir) {
        File archiveFile = new File(databaseDir, CITY_EDITION + ".tar.gz");
        try {
            if (!archiveFile.exists()) {
                log.info("Downloading MaxMind {} database...", CITY_EDITION);
                HttpURLConnection connection = (HttpURLConnection) URI.create(
                        DATABASE_DOWNLOAD_ENDPOINT.formatted(CITY_EDITION, this.properties.getLicense())
                ).toURL().openConnection();
                int status = connection.getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) {
                    log.warn(
                            "MaxMind returned HTTP {} for {} ({}); skipping download",
                            status,
                            CITY_EDITION,
                            status == 429 ? "rate limited" : "check MaxMind status"
                    );
                    return false;
                }

                try (
                        InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                        FileOutputStream outputStream = new FileOutputStream(archiveFile)
                ) {
                    inputStream.transferTo(outputStream);
                }
            }

            extractDatabase(archiveFile, databaseDir, databaseFile);
            Files.deleteIfExists(archiveFile.toPath());
            return true;
        } catch (IOException ex) {
            log.warn("Failed to download MaxMind database: {}", ex.getMessage());
            archiveFile.delete();
            return false;
        }
    }

    @SneakyThrows
    private void extractDatabase(File archiveFile, File databaseDir, File databaseFile) {
        log.info("Extracting MaxMind {} database...", CITY_EDITION);
        try (
                InputStream fileInputStream = Files.newInputStream(archiveFile.toPath());
                GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(fileInputStream);
                TarArchiveInputStream tarInputStream = new TarArchiveInputStream(gzipInputStream)
        ) {
            TarArchiveEntry entry;
            while ((entry = tarInputStream.getNextEntry()) != null) {
                if (!entry.isFile() || !entry.getName().endsWith(".mmdb")) {
                    continue;
                }
                Files.copy(tarInputStream, databaseFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                log.info("Extracted MaxMind database to {}", databaseFile.getAbsolutePath());
                return;
            }
        }
        throw new IOException("Could not find .mmdb file in MaxMind archive");
    }
}
