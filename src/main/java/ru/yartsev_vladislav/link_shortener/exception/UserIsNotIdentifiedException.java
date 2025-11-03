package ru.yartsev_vladislav.link_shortener.exception;

public class UserIsNotIdentifiedException extends Exception {
    public UserIsNotIdentifiedException(String userId) {
        super(String.format("User with id '%s' is not identified", userId));
    }
}
