package com.kidsstory;

import com.kidsstory.config.AppProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class KidsStoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(KidsStoryApplication.class, args);
    }

    /** Ensures the local media directory exists before the static resource handler serves from it. */
    @Bean
    public CommandLineRunner ensureMediaRoot(AppProperties appProperties) {
        return args -> {
            try {
                Files.createDirectories(Path.of(appProperties.getMediaRoot()));
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to create media root directory.", ex);
            }
        };
    }
}
