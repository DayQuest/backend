package com.dayquest.dayquestbackend.user.models;
import com.dayquest.dayquestbackend.quest.Quest;
import com.dayquest.dayquestbackend.user.Punishments;
import com.dayquest.dayquestbackend.video.models.Video;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Entity
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false, unique = true)
    private String username;

    //This is just for beta: every interaction such as liking, commenting etc. will increase this to
    //have a look at the beta tester activities.
    private int interactions;
    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;


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

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "verification_expiration")
    private LocalDateTime verificationCodeExpiresAt;

    private boolean enabled;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_authorities", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "authority")
    private List<String> authorities = new ArrayList<>();

    private int followers;

    @ManyToOne
    @JoinColumn(name = "daily_quest_id")
    private Quest dailyQuest;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Video> postedVideos = new ArrayList<>();

    @Lob
    @Column
    private byte[] profilePicture;

    private String adminComment;

    public byte[] getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(byte[] profilePicture) {
        this.profilePicture = profilePicture;
    }

    public User() {
        this.postedVideos = new ArrayList<>();
    }


    public void addPostedVideo(Video video) {
        postedVideos.add(video);
        video.setUser(this);
    }

    public List<Video> getPostedVideos() {
        return postedVideos;
    }

    public void setPostedVideos(List<Video> postedVideos) {
        this.postedVideos = postedVideos;
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

    public Quest getDailyQuest() {
        return dailyQuest;
    }

    public void setDailyQuest(Quest dailyQuest) {
        this.dailyQuest = dailyQuest;
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
}
