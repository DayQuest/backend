package com.example.dayquest.model;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;


@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Nullable
    private String[] disliked;

    @Nullable
    private String[] likedHashtags;

    @ManyToOne
    @JoinColumn(name = "daily_quest_id")
    private Quest dailyQuest;

    // Getter und Setter

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String[] getDisliked() {
        return disliked;
    }

    public void setDisliked(String[] disliked) {
        this.disliked = disliked;
    }

    public String[] getLikedHashtags() {
        return likedHashtags;
    }

    public void setLikedHashtags(String[] likedHashtags) {
        this.likedHashtags = likedHashtags;
    }

    public Quest getDailyQuest() {
        return dailyQuest;
    }

    public void setDailyQuest(Quest dailyQuest) {
        this.dailyQuest = dailyQuest;
    }
}
