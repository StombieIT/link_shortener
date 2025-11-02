package ru.yartsev_vladislav.link_shortener.entity;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "links")
public class Link {

    @Id
    private String slug;

    @Nonnull
    private String fullUrl;

    @Nullable
    private Integer attemptsLimit;

    private int attempts;

    @Nonnull
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    public Link() {}

    public Link(String slug, String fullUrl, User owner, Integer limit, Integer attempts) {
        this(slug, fullUrl, owner, limit);
        this.attempts = attempts;
    }

    public Link(String slug, String fullUrl, User owner, Integer attemptsLimit) {
        this(slug, fullUrl, owner);
        this.attemptsLimit = attemptsLimit;
    }

    public Link(String slug, String fullUrl, User owner) {
        this.slug = slug;
        this.fullUrl = fullUrl;
        this.owner = owner;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    @Nonnull
    public String getFullUrl() {
        return fullUrl;
    }

    public void setFullUrl(@Nonnull String fullUrl) {
        this.fullUrl = fullUrl;
    }

    @Nullable
    public Integer getAttemptsLimit() {
        return attemptsLimit;
    }

    public void setAttemptsLimit(@Nullable Integer attemptsLimit) {
        this.attemptsLimit = attemptsLimit;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(@Nullable Integer attempts) {
        this.attempts = attempts;
    }

    @Nonnull
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
