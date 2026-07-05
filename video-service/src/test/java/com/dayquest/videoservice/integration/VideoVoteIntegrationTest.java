package com.dayquest.videoservice.integration;

import com.dayquest.videoservice.model.Video;
import com.dayquest.videoservice.model.VideoStatus;
import com.dayquest.videoservice.repository.VideoRepository;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class VideoVoteIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VideoRepository videoRepository;

    @MockBean
    private MinioClient minioClient;

    @Test
    void shouldUpvoteVideo() throws Exception {
        // Given
        UUID userUuid = UUID.randomUUID();
        Video video = new Video();
        video.setUuid(UUID.randomUUID());
        video.setUserUuid(UUID.randomUUID());
        video.setTitle("Vote Test Video");
        video.setStorageUrl("http://localhost/minio/test2.mp4");
        video.setStatus(VideoStatus.READY);
        video.setCreatedAt(LocalDateTime.now());
        video.setUpdatedAt(LocalDateTime.now());
        
        videoRepository.save(video);

        // When & Then
        mockMvc.perform(post("/videos/" + video.getUuid() + "/vote")
                .param("isUpvote", "true")
                .header("X-User-Id", userUuid.toString()))
                .andExpect(status().isOk());
    }
}
