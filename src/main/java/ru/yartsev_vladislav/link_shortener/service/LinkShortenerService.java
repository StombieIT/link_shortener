package ru.yartsev_vladislav.link_shortener.service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.yartsev_vladislav.link_shortener.config.LinkConfig;
import ru.yartsev_vladislav.link_shortener.entity.Link;
import ru.yartsev_vladislav.link_shortener.entity.User;
import ru.yartsev_vladislav.link_shortener.exception.LinkDoesNotExistException;
import ru.yartsev_vladislav.link_shortener.exception.LinkHasExpiredException;
import ru.yartsev_vladislav.link_shortener.exception.LinkLimitExceededException;
import ru.yartsev_vladislav.link_shortener.exception.NotExpiredLinkAlreadyExistsException;
import ru.yartsev_vladislav.link_shortener.exception.UserDoesNotExistException;
import ru.yartsev_vladislav.link_shortener.exception.UserHasNotEnoughRightsException;
import ru.yartsev_vladislav.link_shortener.exception.UserIsNotIdentifiedException;
import ru.yartsev_vladislav.link_shortener.model.CreateLinkOptions;
import ru.yartsev_vladislav.link_shortener.model.CreateLinkResult;
import ru.yartsev_vladislav.link_shortener.model.EditLinkOptions;
import ru.yartsev_vladislav.link_shortener.repository.LinkRepository;
import ru.yartsev_vladislav.link_shortener.repository.UserRepository;

@Component
public class LinkShortenerService {
  private final UserRepository userRepository;
  private final LinkRepository linkRepository;
  private final UrlService urlService;
  private final LinkConfig linkConfig;

  @Autowired
  public LinkShortenerService(
      UserRepository userRepository,
      LinkRepository linkRepository,
      UrlService urlService,
      LinkConfig linkConfig) {
    this.userRepository = userRepository;
    this.linkRepository = linkRepository;
    this.urlService = urlService;
    this.linkConfig = linkConfig;
  }

  public CreateLinkResult createLink(CreateLinkOptions options, String userId)
      throws UserDoesNotExistException, NotExpiredLinkAlreadyExistsException {
    User owner = ensureUser(userId);
    String url = options.url;
    Integer limit = options.limit;

    urlService.validateUrl(url);
    validateLimit(limit);

    Optional<Link> notExpiredLink =
        linkRepository.findByFullUrlAndOwnerIdAndCreatedAtGreaterThanEqual(
            url, owner.getId(), LocalDateTime.now().minusSeconds(linkConfig.getTimeToLeave()));
    if (notExpiredLink.isPresent()) {
      throw new NotExpiredLinkAlreadyExistsException(notExpiredLink.get());
    }

    String slug = urlService.generateLinkSlug(url, owner.getId());

    Optional<Link> linkFromDb = linkRepository.findById(slug);
    if (linkFromDb.isPresent() && isLinkExpired(linkFromDb.get())) {
      linkRepository.delete(linkFromDb.get());
    }

    Link link = new Link(slug, url, owner);
    if (limit != null) {
      link.setAttemptsLimit(limit);
    }
    link = linkRepository.save(link);
    String shortUrl = urlService.generateShortUrl(link.getSlug());

    return new CreateLinkResult(owner.getId(), shortUrl);
  }

  public String getFullLink(String slug)
      throws LinkDoesNotExistException, LinkHasExpiredException, LinkLimitExceededException {
    Optional<Link> optionalLink = linkRepository.findById(slug);
    if (optionalLink.isEmpty()) {
      throw new LinkDoesNotExistException(slug);
    }
    Link link = optionalLink.get();

    validateLinkExpiration(link);
    validateLinkLimitExceeding(link);

    link.setAttempts(link.getAttempts() + 1);
    linkRepository.save(link);

    return link.getFullUrl();
  }

  public void deleteLink(String slug, String ownerId)
      throws UserHasNotEnoughRightsException,
          LinkDoesNotExistException,
          UserIsNotIdentifiedException {
    Link link = ensureLinkWithOwner(slug, ownerId);

    linkRepository.delete(link);
  }

  public void editLink(String slug, String ownerId, EditLinkOptions options)
      throws UserHasNotEnoughRightsException,
          LinkDoesNotExistException,
          UserIsNotIdentifiedException,
          LinkHasExpiredException {
    Link link = ensureLinkWithOwner(slug, ownerId);

    validateLinkExpiration(link);
    validateLimit(options.limit);

    link.setAttemptsLimit(options.limit);
    linkRepository.save(link);
  }

  @Scheduled(fixedRateString = "${scheduler.link-cleanup-delay-ms}")
  @Transactional
  public void cleanupExpiredLinks() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime expiredLinksCreatedAtThreshold = now.minusSeconds(linkConfig.getTimeToLeave());

    linkRepository.deleteAllByCreatedAtBefore(expiredLinksCreatedAtThreshold);
  }

  protected User ensureUser(String userId) throws UserDoesNotExistException {
    if (userId == null) {
      User user = new User();
      return userRepository.save(user);
    }

    Optional<User> user = userRepository.findById(userId);
    if (user.isEmpty()) {
      throw new UserDoesNotExistException(userId);
    }

    return user.get();
  }

  protected Link ensureLinkWithOwner(String slug, String ownerId)
      throws UserIsNotIdentifiedException,
          LinkDoesNotExistException,
          UserHasNotEnoughRightsException {
    if (ownerId == null) {
      throw new UserIsNotIdentifiedException(ownerId);
    }

    Optional<Link> optionalLink = linkRepository.findById(slug);
    if (optionalLink.isEmpty()) {
      throw new LinkDoesNotExistException(slug);
    }

    Link link = optionalLink.get();
    if (!link.getOwner().getId().equals(ownerId)) {
      throw new UserHasNotEnoughRightsException(ownerId);
    }

    return link;
  }

  protected void validateLimit(Integer limit) {
    if (limit != null && limit <= 0) {
      throw new IllegalArgumentException("Limit should more than 0");
    }
  }

  protected boolean isLinkExpired(Link link) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime expirationTime = link.getCreatedAt().plusSeconds(linkConfig.getTimeToLeave());
    return now.isAfter(expirationTime);
  }

  protected void validateLinkLimitExceeding(Link link) throws LinkLimitExceededException {
    if (link.getAttemptsLimit() != null && link.getAttempts() >= link.getAttemptsLimit()) {
      throw new LinkLimitExceededException(link);
    }
  }

  private void validateLinkExpiration(Link link) throws LinkHasExpiredException {
    if (isLinkExpired(link)) {
      throw new LinkHasExpiredException(link);
    }
  }
}
