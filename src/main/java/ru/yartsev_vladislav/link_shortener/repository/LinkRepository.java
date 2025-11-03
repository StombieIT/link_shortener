package ru.yartsev_vladislav.link_shortener.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yartsev_vladislav.link_shortener.entity.Link;

import java.time.LocalDateTime;
import java.util.Optional;

public interface LinkRepository extends JpaRepository<Link, String> {
    Optional<Link> findByFullUrlAndOwnerIdAndCreatedAtGreaterThanEqual(
            String fullUrl,
            String ownerId,
            LocalDateTime createdAt
    );

    void deleteAllByCreatedAtBefore(LocalDateTime createdAt);
}
