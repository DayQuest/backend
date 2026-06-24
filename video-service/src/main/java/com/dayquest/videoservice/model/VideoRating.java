package com.dayquest.videoservice.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "video_ratings")
@IdClass(VideoRatingId.class)

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
public class VideoRating {

    @Id
    @Column(name = "user_uuid")
    private UUID userUuid;

    @Id
    @Column(name = "video_uuid")
    private UUID videoUuid;

    @Column(nullable = false)
    private boolean isUpvote;














}

