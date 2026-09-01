package com.kidsstory.config;

import com.kidsstory.service.LocalStorageService;
import com.kidsstory.service.R2StorageService;
import com.kidsstory.service.StorageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Picks exactly one StorageService impl, mirroring the Python _build_storage_backend() factory. */
@Configuration
public class StorageConfig {

    @Bean
    public StorageService storageService(AppProperties appProperties) {
        if ("r2".equals(appProperties.getStorageBackend())) {
            return new R2StorageService(appProperties);
        }
        return new LocalStorageService(appProperties);
    }
}
