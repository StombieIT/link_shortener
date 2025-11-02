package ru.yartsev_vladislav.link_shortener.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yartsev_vladislav.link_shortener.entity.User;

public interface UserRepository extends JpaRepository<User, String> {
}
