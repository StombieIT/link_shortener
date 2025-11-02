package ru.yartsev_vladislav.link_shortener.exception;

public class UserDoesNotExistException extends Exception {
    public UserDoesNotExistException(String userId) {
        super(String.format("User with id '%s' does not exist", userId));
    }
}
