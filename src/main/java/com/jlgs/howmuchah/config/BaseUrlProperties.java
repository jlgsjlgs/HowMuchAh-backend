package com.jlgs.howmuchah.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class BaseUrlProperties {
    private String baseUrl;
}