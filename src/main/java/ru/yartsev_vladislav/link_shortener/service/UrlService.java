package ru.yartsev_vladislav.link_shortener.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yartsev_vladislav.link_shortener.config.UrlConfig;
import ru.yartsev_vladislav.link_shortener.exception.UrlIsNotValidException;

import java.util.regex.Pattern;

@Component
public class UrlService {
    private static final String BASE62_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int SLUG_LENGTH = 8;

    private static long getHash(String a, String b) {
        long hash = 0;
        // Полиномиальный хеш для первой строки
        for (char c : a.toCharArray()) {
            hash = hash * 31 + c;
        }

        // Комбинируем вторую строку через XOR с другим множителем
        long hashB = 0;
        for (char c : b.toCharArray()) {
            hashB = hashB * 37 + c;
        }

        // Объединяем два хеша через XOR, чтобы избежать переполнения
        hash ^= hashB;

        // Делаем положительным
        return hash & 0x7FFFFFFFFFFFFFFFL;
    }

    // Кодирование хэша в base62-формат
    private static String encodeBase62(long value, int length) {
        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            sb.append(BASE62_ALPHABET.charAt((int)(value % 62)));
            value /= 62;
        }
        while (sb.length() < length) {
            sb.append('0'); // дополняем до нужной длины
        }
        return sb.reverse().toString();
    }

    private final UrlConfig urlConfig;

    @Autowired
    public UrlService(UrlConfig urlConfig) {
        this.urlConfig = urlConfig;
    }

    public String generateLinkSlug(String url, String salt) {
        long hash = getHash(url, salt);
        return encodeBase62(hash, SLUG_LENGTH);
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
