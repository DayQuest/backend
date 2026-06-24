package com.dayquest.userservice.services;

import com.dayquest.userservice.models.User;
import com.dayquest.userservice.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Migration script to move profile pictures from PostgreSQL BLOB to MinIO.
 *
 * Enable by setting: migration.profile-pictures.enabled=true
 *
 * This runs once at startup and migrates all users with profile pictures
 * stored in the database to MinIO, then clears the database BLOB.
 */
@Component
@ConditionalOnProperty(name = "migration.profile-pictures.enabled", havingValue = "true")
public class ProfilePictureMigration implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ProfilePictureMigration.class);
    private static final int BATCH_SIZE = 50;

    private final UserRepository userRepository;
    private final ProfilePictureService profilePictureService;

    @Autowired
    public ProfilePictureMigration(UserRepository userRepository,
                                    ProfilePictureService profilePictureService) {
        this.userRepository = userRepository;
        this.profilePictureService = profilePictureService;
    }

    @Override
    public void run(String... args) {
        if (!profilePictureService.isMinioAvailable()) {
            logger.warn("MinIO is not available. Skipping profile picture migration.");
            return;
        }

        logger.info("Starting profile picture migration from PostgreSQL to MinIO...");

        int migratedCount = 0;
        int failedCount = 0;
        int page = 0;

        Page<User> userPage;
        do {
            userPage = userRepository.findUsersWithProfilePicture(PageRequest.of(page, BATCH_SIZE));

            for (User user : userPage.getContent()) {
                try {
                    byte[] profilePicture = user.getProfilePicture();

                    if (profilePicture == null || profilePicture.length == 0) {
                        continue;
                    }

                    // Check if already migrated to MinIO
                    if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
                        logger.debug("User {} already has MinIO URL, clearing DB BLOB", user.getUsername());
                        user.setProfilePicture(null);
                        userRepository.save(user);
                        continue;
                    }

                    // Upload to MinIO
                    String url = profilePictureService.uploadProfilePicture(user.getUuid(), profilePicture).join();

                    if (url != null) {
                        user.setProfilePictureUrl(url);
                        user.setProfilePicture(null); // Clear DB BLOB
                        userRepository.save(user);
                        logger.debug("Migrated profile picture for user {} to {}", user.getUsername(), url);
                        migratedCount++;
                    } else {
                        throw new RuntimeException("MinIO upload returned null URL");
                    }
                } catch (Exception e) {
                    logger.error("Failed to migrate profile picture for user {}: {}",
                            user.getUsername(), e.getMessage());
                    failedCount++;
                }
            }

            page++;
            logger.info("Migrated batch {}: {} users processed", page, userPage.getNumberOfElements());

        } while (userPage.hasNext());

        logger.info("Profile picture migration completed. Migrated: {}, Failed: {}",
                migratedCount, failedCount);
    }
}




