package org.cinemaabyss.proxy.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "proxy")
public record ProxyProperties(
        @NotBlank String monolithUrl,
        @NotBlank String moviesServiceUrl,
        @NotBlank String eventsServiceUrl,
        boolean gradualMigration,
        @Min(0) @Max(100) int moviesMigrationPercent
) {
}
