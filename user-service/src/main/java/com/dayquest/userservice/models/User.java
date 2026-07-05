package com.dayquest.userservice.models;

import com.dayquest.userservice.enums.Punishments;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Entity
@Table(name = "user_data")
public class User implements UserDetails {

    @Id
    @GeneratedValue
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID uuid;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false, unique = true)
    private String username;

    private int interactions;
    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;


    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(255)")
    private Punishments punishment;

    @ElementCollection
    @CollectionTable(name = "user_badge", joinColumns = @JoinColumn(name = "user_id"))
    private List<UUID> badges;

    private int leftRerolls;
    private LocalDateTime lastReroll;

    private String passwordResetToken;

    @ElementCollection
    @CollectionTable(name = "user_done_quest", joinColumns = @JoinColumn(name = "user_id"))
    private List<UUID> doneQuests;

    private LocalDateTime lastLogin;

    private LocalDateTime passwordResetTokenExpiry;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "verification_expiration")
    private LocalDateTime verificationCodeExpiresAt;

    private boolean enabled;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_authorities", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "authority")
    private List<String> authorities = new ArrayList<>();

    private int followers;


    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    private String adminComment;

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public int getInteractions() {
        return interactions;
    }

    public void increaseInteractions() {
        setInteractions(getInteractions() + 1);
    }

    public void setInteractions(int interactions) {
        this.interactions = interactions;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Punishments getPunishment() {
        return punishment;
    }

    public void setPunishment(Punishments punishment) {
        this.punishment = punishment;
    }

    public int getFollowers() {
        return followers;
    }

    public void setFollowers(int followers) {
        this.followers = followers;
    }

    public String getAdminComment() {
        return adminComment;
    }

    public void setAdminComment(String adminComment) {
        this.adminComment = adminComment;
    }

    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public void setPasswordResetToken(String passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public LocalDateTime getVerificationCodeExpiresAt() {
        return verificationCodeExpiresAt;
    }

    public void setVerificationCodeExpiresAt(LocalDateTime verificationCodeExpiresAt) {
        this.verificationCodeExpiresAt = verificationCodeExpiresAt;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities == null ? List.of() :
                authorities.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
    }


    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public List<UUID> getDoneQuests() {
        return doneQuests;
    }

    public void setDoneQuests(List<UUID> doneQuests) {
        this.doneQuests = doneQuests;
    }

    public void addDoneQuest(UUID questId) {
        doneQuests.add(questId);
    }

    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }

    public List<String> getAuthoritiesList() {
        return authorities;
    }


    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public int getLeftRerolls() {
        return leftRerolls;
    }

    public void setLeftRerolls(int leftRerolls) {
        this.leftRerolls = leftRerolls;
    }

    public LocalDateTime getLastReroll() {
        return lastReroll;
    }

    public void setLastReroll(LocalDateTime lastReroll) {
        this.lastReroll = lastReroll;
    }

    public List<UUID> getBadges() {
        return badges;
    }

    public void setBadges(List<UUID> badges) {
        this.badges = badges;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPasswordResetTokenExpiry() {
        return passwordResetTokenExpiry;
    }

    public void setPasswordResetTokenExpiry(LocalDateTime passwordResetTokenExpiry) {
        this.passwordResetTokenExpiry = passwordResetTokenExpiry;
    }
}
