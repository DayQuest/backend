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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class VideoIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VideoRepository videoRepository;

    @MockBean
    private MinioClient minioClient; // Mocking Minio to avoid requiring a real Minio instance for DB tests

    @Test
    void shouldRetrieveVideosFromDatabase() throws Exception {
        // Given
        UUID userUuid = UUID.randomUUID();
        UUID questUuid = UUID.randomUUID();

        Video video = new Video();
        video.setUuid(UUID.randomUUID());
        video.setUserUuid(userUuid);
        video.setQuestUuid(questUuid);
        video.setTitle("Integration Test Video");
        video.setDescription("Integration Description");
        video.setStorageUrl("http://localhost/minio/test.mp4");
        video.setStatus(VideoStatus.READY);
        video.setCreatedAt(LocalDateTime.now());
        video.setUpdatedAt(LocalDateTime.now());
        
        videoRepository.save(video);

        // When & Then
        mockMvc.perform(get("/videos")
                .param("userUuid", userUuid.toString())
                .header("X-User-Id", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Integration Test Video"))
                .andExpect(jsonPath("$.content[0].description").value("Integration Description"));
    }
}
