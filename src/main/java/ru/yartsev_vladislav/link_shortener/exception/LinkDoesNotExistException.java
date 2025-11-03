package ru.yartsev_vladislav.link_shortener.exception;

public class LinkDoesNotExistException extends Exception {
  public LinkDoesNotExistException(String slug) {
    super(String.format("Link '/%s' does not exist", slug));
  }
}
