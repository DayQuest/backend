package com.dayquest.contentservice.model;

import jakarta.persistence.*;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "badges")

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    private String iconUrl;

    @ElementCollection
    @CollectionTable(name = "badge_users", joinColumns = @JoinColumn(name = "badge_id"))
    @Column(name = "user_uuid")
    @Builder.Default
    private List<UUID> userUuids = new ArrayList<>();


    public void addUser(UUID userUuid) {
        if (!this.userUuids.contains(userUuid)) {
            this.userUuids.add(userUuid);
        }
    }
}

