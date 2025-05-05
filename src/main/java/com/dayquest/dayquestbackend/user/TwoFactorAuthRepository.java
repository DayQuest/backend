package com.dayquest.dayquestbackend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for accessing TwoFactorAuth entities in the database.
 */
@Repository
public interface TwoFactorAuthRepository extends JpaRepository<TwoFactorAuth, Long> {
    
    /**
     * Finds the 2FA configuration for a specific user.
     *
     * @param user The user whose 2FA configuration is being searched for
     * @return Optional with the 2FA configuration, if available
     */
    Optional<TwoFactorAuth> findByUser(User user);
    
    /**
     * Finds the 2FA configuration for a user by their ID.
     *
     * @param userId The ID of the user
     * @return Optional with the 2FA configuration, if available
     */
    Optional<TwoFactorAuth> findByUserId(Long userId);
    
    /**
     * Finds the 2FA configuration for a user by their username.
     *
     * @param username The username
     * @return Optional with the 2FA configuration, if available
     */
    Optional<TwoFactorAuth> findByUser_Username(String username);
    
    /**
     * Checks if 2FA is enabled for a specific user.
     *
     * @param uuid The UUID of the user
     * @return true if 2FA is enabled, otherwise false
     */
    boolean existsByUser_UuidAndEnabledTrue(UUID uuid);
}
