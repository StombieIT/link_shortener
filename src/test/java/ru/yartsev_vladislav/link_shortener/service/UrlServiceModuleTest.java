package ru.yartsev_vladislav.link_shortener.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.yartsev_vladislav.link_shortener.config.UrlConfig;
import ru.yartsev_vladislav.link_shortener.exception.UrlIsNotValidException;

public class UrlServiceModuleTest {

  private UrlService urlService;
  private UrlConfig urlConfig;

  @BeforeEach
  void setUp() {
    urlConfig = Mockito.mock(UrlConfig.class);
    Mockito.when(urlConfig.getScheme()).thenReturn("http");
    Mockito.when(urlConfig.getHostName()).thenReturn("localhost:8080");

    urlService = new UrlService(urlConfig);
  }

  @Test
  void generateLinkSlug_ShouldReturnSlugOfMinimumLength() {
    String url = "https://example.com";
    String salt = "randomSalt";

    String slug = urlService.generateLinkSlug(url, salt);

    assertNotNull(slug);
    assertTrue(slug.length() >= 8, "Slug length should be at least 8 characters");
  }

  @Test
  void generateSlug_ShouldBeDeterministic() {
    String url = "https://example.com";
    String salt = "randomSalt";

    String slug1 = urlService.generateLinkSlug(url, salt);
    String slug2 = urlService.generateLinkSlug(url, salt);

    assertEquals(slug1, slug2, "Slug generation should be deterministic for same input");
  }

  @Test
  void generateShortUrl_ShouldReturnCorrectFormat() {
    String slug = "abcd1234";
    String shortUrl = urlService.generateShortUrl(slug);

    assertEquals("http://localhost:8080/abcd1234", shortUrl);
  }

  @Test
  void validateUrl_ShouldPassForValidUrls() {
    assertDoesNotThrow(() -> urlService.validateUrl("http://example.com"));
    assertDoesNotThrow(() -> urlService.validateUrl("https://example.com/path"));
    assertDoesNotThrow(() -> urlService.validateUrl("https://example.com:8080/path"));
  }

  @Test
  void validateUrl_ShouldThrowForInvalidUrls() {
    assertThrows(UrlIsNotValidException.class, () -> urlService.validateUrl(null));
    assertThrows(UrlIsNotValidException.class, () -> urlService.validateUrl(""));
    assertThrows(UrlIsNotValidException.class, () -> urlService.validateUrl("   "));
    assertThrows(UrlIsNotValidException.class, () -> urlService.validateUrl("ftp://example.com"));
    assertThrows(UrlIsNotValidException.class, () -> urlService.validateUrl("example.com"));
    assertThrows(UrlIsNotValidException.class, () -> urlService.validateUrl("http//example.com"));
  }
}
