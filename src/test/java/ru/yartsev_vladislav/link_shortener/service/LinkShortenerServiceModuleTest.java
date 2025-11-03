package ru.yartsev_vladislav.link_shortener.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yartsev_vladislav.link_shortener.config.LinkConfig;
import ru.yartsev_vladislav.link_shortener.entity.Link;
import ru.yartsev_vladislav.link_shortener.entity.User;
import ru.yartsev_vladislav.link_shortener.exception.*;
import ru.yartsev_vladislav.link_shortener.model.CreateLinkOptions;
import ru.yartsev_vladislav.link_shortener.model.CreateLinkResult;
import ru.yartsev_vladislav.link_shortener.model.EditLinkOptions;
import ru.yartsev_vladislav.link_shortener.repository.LinkRepository;
import ru.yartsev_vladislav.link_shortener.repository.UserRepository;

public class LinkShortenerServiceModuleTest {

  private LinkRepository linkRepository;
  private UserRepository userRepository;
  private UrlService urlService;
  private LinkConfig linkConfig;
  private LinkShortenerService service;

  @BeforeEach
  void setUp() {
    linkRepository = mock(LinkRepository.class);
    userRepository = mock(UserRepository.class);
    urlService = mock(UrlService.class);
    linkConfig = mock(LinkConfig.class);

    when(linkConfig.getTimeToLeave()).thenReturn(3600L); // 1 hour TTL

    service = new LinkShortenerService(userRepository, linkRepository, urlService, linkConfig);
  }

  @Test
  void createLink_ShouldCreateAndReturnShortUrl() throws Exception {
    String userId = "user1";
    User user = new User();
    user.setId(userId);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(urlService.generateLinkSlug(anyString(), anyString())).thenReturn("slug1234");
    when(urlService.generateShortUrl("slug1234")).thenReturn("http://short.url/slug1234");

    CreateLinkOptions options = new CreateLinkOptions();
    options.url = "https://example.com";
    options.limit = 5;

    when(linkRepository.findByFullUrlAndOwnerIdAndCreatedAtGreaterThanEqual(
            anyString(), anyString(), any()))
        .thenReturn(Optional.empty());
    when(linkRepository.findById("slug1234")).thenReturn(Optional.empty());

    Link savedLink = new Link("slug1234", options.url, user);
    savedLink.setCreatedAt(LocalDateTime.now());
    when(linkRepository.save(any(Link.class))).thenReturn(savedLink);

    CreateLinkResult result = service.createLink(options, userId);

    assertEquals(userId, result.userId);
    assertEquals("http://short.url/slug1234", result.shortUrl);
    verify(linkRepository, times(1)).save(any(Link.class));
  }

  @Test
  void createLink_ShouldThrowIfLinkExistsAndNotExpired() throws Exception {
    String userId = "user1";
    User user = new User();
    user.setId(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    Link existingLink = new Link("slug", "https://example.com", user);
    existingLink.setCreatedAt(LocalDateTime.now());
    when(linkRepository.findByFullUrlAndOwnerIdAndCreatedAtGreaterThanEqual(
            anyString(), anyString(), any()))
        .thenReturn(Optional.of(existingLink));

    CreateLinkOptions options = new CreateLinkOptions();
    options.url = "https://example.com";

    assertThrows(
        NotExpiredLinkAlreadyExistsException.class, () -> service.createLink(options, userId));
  }

  @Test
  void getFullLink_ShouldReturnUrlAndIncrementAttempts() throws Exception {
    User user = new User();
    user.setId("user1");
    Link link = new Link("slug", "https://example.com", user);
    link.setAttempts(0);
    link.setCreatedAt(LocalDateTime.now());

    when(linkRepository.findById("slug")).thenReturn(Optional.of(link));

    String url = service.getFullLink("slug");

    assertEquals("https://example.com", url);
    assertEquals(1, link.getAttempts());
    verify(linkRepository).save(link);
  }

  @Test
  void getFullLink_ShouldThrowIfLinkDoesNotExist() {
    when(linkRepository.findById("slug")).thenReturn(Optional.empty());
    assertThrows(LinkDoesNotExistException.class, () -> service.getFullLink("slug"));
  }

  @Test
  void getFullLink_ShouldThrowIfAttemptsExceeded() {
    User user = new User();
    Link link = new Link("slug", "https://example.com", user);
    link.setAttempts(5);
    link.setAttemptsLimit(5);
    link.setCreatedAt(LocalDateTime.now());

    when(linkRepository.findById("slug")).thenReturn(Optional.of(link));

    assertThrows(LinkLimitExceededException.class, () -> service.getFullLink("slug"));
  }

  @Test
  void deleteLink_ShouldDeleteSuccessfully() throws Exception {
    User user = new User();
    user.setId("user1");
    Link link = new Link("slug", "https://example.com", user);
    link.setCreatedAt(LocalDateTime.now());

    when(linkRepository.findById("slug")).thenReturn(Optional.of(link));

    service.deleteLink("slug", "user1");

    verify(linkRepository).delete(link);
  }

  @Test
  void editLink_ShouldUpdateLimit() throws Exception {
    User user = new User();
    user.setId("user1");
    Link link = new Link("slug", "https://example.com", user);
    link.setCreatedAt(LocalDateTime.now());

    when(linkRepository.findById("slug")).thenReturn(Optional.of(link));

    EditLinkOptions options = new EditLinkOptions();
    options.limit = 10;

    service.editLink("slug", "user1", options);

    assertEquals(10, link.getAttemptsLimit());
    verify(linkRepository).save(link);
  }

  @Test
  void ensureLinkWithOwner_ShouldThrowIfOwnerMismatch() {
    User user = new User();
    user.setId("user1");
    Link link = new Link("slug", "https://example.com", user);
    link.setCreatedAt(LocalDateTime.now());

    when(linkRepository.findById("slug")).thenReturn(Optional.of(link));

    assertThrows(
        UserHasNotEnoughRightsException.class, () -> service.ensureLinkWithOwner("slug", "user2"));
  }
}
