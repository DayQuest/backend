package com.dayquest.dayquestbackend.beta;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class BetaKey {

  @Id
  private Long discordId;

  @Column(name = "beta_key")
  private String key;
  private boolean inUse;
  private String appUsername;

  public String getAppUsername() {
    return appUsername;
  }

  public void setUsername(String username) {
    this.appUsername = username;
  }

  public long getDiscordId() {
    return discordId;
  }

  public void setDiscordId(long discordId) {
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
