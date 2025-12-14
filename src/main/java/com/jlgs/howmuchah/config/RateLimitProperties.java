package com.jlgs.howmuchah.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    /**
     * Maximum number of requests allowed per time window
     */
    private int capacity = 60;

    /**
     * Time window in minutes
     */
    private int windowMinutes = 1;

    /**
     * Whether rate limiting is enabled
     */
    private boolean enabled = true;
}