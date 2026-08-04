package com.dayquest.userservice.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_milestones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMilestone {

    @EmbeddedId
    private UserMilestoneId id = new UserMilestoneId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("milestoneId")
    @JoinColumn(name = "milestone_id")
    private Milestone milestone;

    @CreationTimestamp
    @Column(name = "unlocked_at", updatable = false, nullable = false)
    private LocalDateTime unlockedAt;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class UserMilestoneId implements Serializable {
        @Column(name = "user_id")
        private UUID userId;

        @Column(name = "milestone_id")
        private Integer milestoneId;
    }
}
