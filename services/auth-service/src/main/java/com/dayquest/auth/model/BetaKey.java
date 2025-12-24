package com.dayquest.auth.model;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

/**
 * Entity representing beta keys for early access.
 */
@Entity
@Table(name = "beta_keys")
public class BetaKey {

    @Id
    @GeneratedValue
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(unique = true, nullable = false)
    private String key;

    private boolean inUse;
    private String usedByUsername;

    // Constructors
    public BetaKey() {}

    public BetaKey(String key) {
        this.key = key;
        this.inUse = false;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public boolean isInUse() { return inUse; }
    public void setInUse(boolean inUse) { this.inUse = inUse; }

    public String getUsedByUsername() { return usedByUsername; }
    public void setUsedByUsername(String usedByUsername) { this.usedByUsername = usedByUsername; }
}
