package com.dayquest.dayquestbackend.user;
import com.dayquest.dayquestbackend.quest.Quest;
import com.dayquest.dayquestbackend.video.Video;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.stream.Collectors;


@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private boolean banned = false;

    @ElementCollection
    @CollectionTable(name = "disliked_quests", joinColumns = @JoinColumn(name = "user_id"))
    private List<UUID> dislikedQuests;

    @ElementCollection
    @CollectionTable(name = "disliked_videos", joinColumns = @JoinColumn(name = "user_id"))
    private List<UUID> dislikedVideos;

    @ElementCollection
    @CollectionTable(name = "liked_quests", joinColumns = @JoinColumn(name = "user_id"))
    private List<UUID> likedQuests;

    @ElementCollection
    @CollectionTable(name = "liked_videos", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "video_id")
    private List<UUID> likedVideos;

    @ElementCollection
    private List<UUID> followedUsers;

    @ElementCollection
    private List<UUID> followerList;

    private String betaKey;

    private String passwordResetToken;

    @ElementCollection
    private List<UUID> doneQuests;

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
    @Column(columnDefinition = "LONGBLOB")
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
        this.dislikedQuests = new ArrayList<>();
        this.dislikedVideos = new ArrayList<>();
        this.likedQuests = new ArrayList<>();
        this.likedVideos = new ArrayList<>();
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

    public String getBetaKey() {
        return betaKey;
    }

    public void setBetaKey(String betaKey) {
        this.betaKey = betaKey;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
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

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public List<UUID> getDislikedQuests() {
        return dislikedQuests;
    }

    public void setDislikedQuests(List<UUID> dislikedQuests) {
        this.dislikedQuests = dislikedQuests;
    }

    public List<UUID> getDislikedVideos() {
        return dislikedVideos;
    }

    public void setDislikedVideos(List<UUID> dislikedVideos) {
        this.dislikedVideos = dislikedVideos;
    }

    public List<UUID> getFollowedUsers() {
        return followedUsers;
    }

    public void setFollowedUsers(List<UUID> followedUsers) {
        this.followedUsers = followedUsers;
    }

    public int getFollowers() {
        return followers;
    }

    public void setFollowers(int followers) {
        this.followers = followers;
    }

    public List<UUID> getLikedQuests() {
        return likedQuests;
    }

    public void setLikedQuests(List<UUID> likedQuests) {
        this.likedQuests = likedQuests;
    }

    public List<UUID> getLikedVideos() {
        return likedVideos;
    }

    public void setLikedVideos(List<UUID> likedVideos) {
        this.likedVideos = likedVideos;
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

    public List<UUID> getFollowerList() {
        return followerList;
    }

    public void setFollowerList(List<UUID> followerList) {
        this.followerList = followerList;
    }

    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }

    public List<String> getAuthoritiesList() {
        return authorities;
    }
}
