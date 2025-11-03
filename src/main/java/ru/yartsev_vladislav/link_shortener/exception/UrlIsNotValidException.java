package ru.yartsev_vladislav.link_shortener.exception;

public class UrlIsNotValidException extends IllegalArgumentException {
  public UrlIsNotValidException(String url) {
    super(String.format("Url '%s' is not valid", url));
  }
}
