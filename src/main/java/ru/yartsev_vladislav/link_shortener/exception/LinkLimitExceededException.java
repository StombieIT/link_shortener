package ru.yartsev_vladislav.link_shortener.exception;

import ru.yartsev_vladislav.link_shortener.entity.Link;

public class LinkLimitExceededException extends Exception {
  public LinkLimitExceededException(Link link) {
    super(String.format("Link '/%s' limit exceeded", link.getSlug()));
  }
}
