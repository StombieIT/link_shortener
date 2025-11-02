package ru.yartsev_vladislav.link_shortener.exception;

import ru.yartsev_vladislav.link_shortener.entity.User;

public class UserIsNotIdentifiedException extends Exception {
    public UserIsNotIdentifiedException(String userId) {
        super(String.format("User '%s' is not identified", userId));
    }
}
