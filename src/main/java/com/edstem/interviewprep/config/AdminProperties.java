package com.edstem.interviewprep.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(String email, String password) {

    public AdminProperties {
        if (StringUtils.hasText(email) != StringUtils.hasText(password)) {
            throw new IllegalArgumentException("Set both app.admin.email and app.admin.password, or neither");
        }
    }

    public boolean isConfigured() {
        return StringUtils.hasText(email);
    }
}
