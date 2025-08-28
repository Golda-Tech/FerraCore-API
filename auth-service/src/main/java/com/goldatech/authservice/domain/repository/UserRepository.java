package com.goldatech.authservice.domain.repository;

import com.goldatech.authservice.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for the User entity.
 * This interface provides methods for interacting with the user data in the database.
 */
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * Finds a user by their email.
     * @param email The email of the user to find.
     * @return An Optional containing the user if found, or an empty Optional otherwise.
     */
    Optional<User> findByEmail(String email);
}
