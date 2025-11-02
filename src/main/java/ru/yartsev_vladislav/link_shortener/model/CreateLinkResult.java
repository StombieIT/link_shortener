package ru.yartsev_vladislav.link_shortener.model;

public class CreateLinkResult {
    public String userId;
    public String shortUrl;

    public CreateLinkResult(String userId, String shortUrl) {
        this.userId = userId;
        this.shortUrl = shortUrl;
    }
}
