package com.dayquest.auth.repository;

import com.dayquest.auth.model.AuthCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthCredentialRepository extends JpaRepository<AuthCredential, UUID> {

    Optional<AuthCredential> findByVerificationCode(String verificationCode);

    Optional<AuthCredential> findByPasswordResetToken(String passwordResetToken);
}
