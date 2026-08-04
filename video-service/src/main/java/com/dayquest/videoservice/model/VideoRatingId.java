package com.dayquest.videoservice.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;


@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
public class VideoRatingId implements Serializable {
    private UUID userUuid;
    private UUID videoUuid;



    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VideoRatingId that = (VideoRatingId) o;
        return Objects.equals(userUuid, that.userUuid) && Objects.equals(videoUuid, that.videoUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userUuid, videoUuid);
    }
}
