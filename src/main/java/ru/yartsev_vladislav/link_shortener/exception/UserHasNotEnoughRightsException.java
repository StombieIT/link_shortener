package ru.yartsev_vladislav.link_shortener.exception;

public class UserHasNotEnoughRightsException extends Exception {
  public UserHasNotEnoughRightsException(String userId) {
    super(String.format("User '%s' has not enough rights", userId));
  }
}
