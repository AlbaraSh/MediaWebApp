package com.mediawebapp.repository;

import com.mediawebapp.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence access for {@link User} entities mapped to the existing {@code users} table.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

	/**
	 * Looks up a user by email, ignoring case so {@code User@x.com} matches {@code user@x.com}.
	 */
	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByUsernameIgnoreCase(String username);
}
