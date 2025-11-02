package ru.yartsev_vladislav.link_shortener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "link")
public class LinkConfig {
    // Выражается в секундах
    private long timeToLeave;

    public long getTimeToLeave() {
        return timeToLeave;
    }

    public void setTimeToLeave(long timeToLeave) {
        this.timeToLeave = timeToLeave;
    }
}
