package ru.yartsev_vladislav.link_shortener.exception;

import ru.yartsev_vladislav.link_shortener.entity.Link;

public class NotExpiredLinkAlreadyExistsException extends Exception {
    public NotExpiredLinkAlreadyExistsException(Link link) {
        super(String.format(
            "Link '%s' for user '%s' already exists and not expired",
            link.getFullUrl(),
            link.getOwner().getId()
        ));
    }
}
