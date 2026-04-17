package com.example.auction.domain.user.repository;

import com.example.auction.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    Optional<User> findByIdAndDeletedFalse(Long userId);

    Optional<User> findByEmailAndDeletedFalse(String email);
}
