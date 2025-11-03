package ru.yartsev_vladislav.link_shortener.exception;

import ru.yartsev_vladislav.link_shortener.entity.Link;

public class LinkHasExpiredException extends Exception {
  public LinkHasExpiredException(Link link) {
    super(String.format("Link '/%s' has expired", link.getSlug()));
  }
}
