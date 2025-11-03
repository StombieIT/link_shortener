package ru.yartsev_vladislav.link_shortener.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.yartsev_vladislav.link_shortener.entity.Link;

public interface LinkRepository extends JpaRepository<Link, String> {
  Optional<Link> findByFullUrlAndOwnerIdAndCreatedAtGreaterThanEqual(
      String fullUrl, String ownerId, LocalDateTime createdAt);

  void deleteAllByCreatedAtBefore(LocalDateTime createdAt);
}
