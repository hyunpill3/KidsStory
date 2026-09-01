package com.kidsstory.config;

import com.kidsstory.service.CleanupService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Replaces Celery beat: schedules CleanupService.purgeExpiredProjects() at
 * app.cleanup-interval-minutes. Registered programmatically (rather than via
 * a property-driven @Scheduled string) so the interval reads directly and
 * type-safely from AppProperties.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulingConfig implements SchedulingConfigurer {

    private final AppProperties appProperties;
    private final CleanupService cleanupService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addFixedRateTask(
                cleanupService::purgeExpiredProjects,
                Duration.ofMinutes(appProperties.getCleanupIntervalMinutes()));
    }
}
