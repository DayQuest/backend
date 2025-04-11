package com.dayquest.dayquestbackend.beta;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class BetaKey {

    @Id
    private Long discordId;

    @Column(name = "beta_key", nullable = false)
    private String key;

    @Column(name = "in_use", nullable = false)
    private boolean inUse;

    @Column(name = "app_username")
    private String appUsername;

    public String getAppUsername() {
        return appUsername;
    }

    public void setUsername(String appUsername) {
        this.appUsername = appUsername;
    }

    public Long getDiscordId() {
        return discordId;
    }

    public void setDiscordId(Long discordId) {
        this.discordId = discordId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isInUse() {
        return inUse;
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }
}

