package ru.yartsev_vladislav.link_shortener.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yartsev_vladislav.link_shortener.config.UrlConfig;
import ru.yartsev_vladislav.link_shortener.exception.UrlIsNotValidException;

import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class UrlService {
    private final UrlConfig urlConfig;

    @Autowired
    public UrlService(UrlConfig urlConfig) {
        this.urlConfig = urlConfig;
    }

    public String generateLinkSlug(String url) {
        // TODO: заменить на реальную реализацию
        return UUID.randomUUID().toString();
    }

    public String generateShortUrl(String slug) {
        return String.format("%s://%s/%s", urlConfig.getScheme(), urlConfig.getHostName(), slug);
    }

    public void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new UrlIsNotValidException(url);
        }

        // Регулярка для простого HTTP/HTTPS URL
        String regex = "^(https?)://([\\w.-]+)(:[0-9]+)?(/.*)?$";
        Pattern pattern = Pattern.compile(regex);

        if (!pattern.matcher(url).matches()) {
            throw new UrlIsNotValidException(url);
        }
    }
}
