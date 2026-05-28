package cc.fascinated.monitor;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@Slf4j
@SpringBootApplication
public class Main {
    @SneakyThrows
    static void main(String[] args) {
        File config = new File("application.yml");
        if (!config.exists()) { // Saving the default config if it doesn't exist locally
            Files.copy(Objects.requireNonNull(Main.class.getResourceAsStream("/application.yml")), config.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("Saved the default configuration to '{}', please re-launch the application", // Log the default config being saved
                    config.getAbsolutePath());
            return;
        }
        log.info("Found configuration at '{}'", config.getAbsolutePath()); // Log the found config

        if (args.length > 0) {
            new SpringApplicationBuilder(Main.class).web(WebApplicationType.NONE).run(args);
        }
        else {
            SpringApplication.run(Main.class, args);
        }
    }
}