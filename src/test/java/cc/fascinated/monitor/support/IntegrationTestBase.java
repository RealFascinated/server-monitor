package cc.fascinated.monitor.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.Field;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegrationTestBase {
    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:18-alpine");
    private static final DockerImageName VICTORIA_METRICS_IMAGE =
            DockerImageName.parse("victoriametrics/victoria-metrics:latest");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withDatabaseName("monitor")
            .withUsername("monitor")
            .withPassword("monitor");

    @Container
    static final GenericContainer<?> VICTORIA_METRICS = new GenericContainer<>(VICTORIA_METRICS_IMAGE)
            .withExposedPorts(8428)
            .withCommand("-storageDataPath=/storage", "-httpListenAddr=:8428");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetIntegrationState() throws Exception {
        this.jdbcTemplate.execute("""
                TRUNCATE TABLE
                    password_reset_tokens,
                    user_preferences,
                    user_sessions,
                    server_folder_assignments,
                    server_folders,
                    server_invites,
                    server_members,
                    server_ingest_tokens,
                    server_inventory,
                    incidents,
                    servers,
                    settings,
                    users
                RESTART IDENTITY CASCADE
                """);
        clearStaticMap("cc.fascinated.monitor.service.SettingsService$Settings", "CACHE");
        clearStaticMap("cc.fascinated.monitor.service.UserPreferencesService$Preferences", "CACHE");
    }

    private static void clearStaticMap(String className, String fieldName) throws Exception {
        Field field = Class.forName(className).getDeclaredField(fieldName);
        field.setAccessible(true);
        ((Map<?, ?>) field.get(null)).clear();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add(
                "monitor.victoriametrics.import-url",
                () -> "http://%s:%d/api/v1/import/prometheus".formatted(
                        VICTORIA_METRICS.getHost(),
                        VICTORIA_METRICS.getMappedPort(8428)
                )
        );
    }
}
