package com.dayquest.videoservice.service;

import com.dayquest.videoservice.model.Video;
import com.dayquest.videoservice.model.VideoStatus;
import com.dayquest.videoservice.model.ViewedVideo;
import com.dayquest.videoservice.repository.VideoRepository;
import com.dayquest.videoservice.repository.ViewedVideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Video Service Tests")
class VideoServiceTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private ViewedVideoRepository viewedVideoRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private VideoService videoService;

    private Video testVideo;
    private UUID testVideoId;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testVideoId = UUID.randomUUID();
        testUserId = UUID.randomUUID();

        testVideo = new Video();
        testVideo.setUuid(testVideoId);
        testVideo.setUserUuid(testUserId);
        testVideo.setTitle("Test Video");
        testVideo.setDescription("Test Description");
        testVideo.setFilePath("/videos/test.mp4");
        testVideo.setThumbnailPath("/thumbnails/test.jpg");
        testVideo.setStatus(VideoStatus.ACTIVE);
        testVideo.setViews(10);
    }

    @Test
    @DisplayName("Should get video by ID")
    void shouldGetVideoById() {
        when(videoRepository.findById(testVideoId)).thenReturn(Optional.of(testVideo));

        Optional<Video> result = videoService.getVideo(testVideoId);

        assertTrue(result.isPresent());
        assertEquals(testVideo.getTitle(), result.get().getTitle());
    }

    @Test
    @DisplayName("Should return empty when video not found")
    void shouldReturnEmptyWhenVideoNotFound() {
        when(videoRepository.findById(testVideoId)).thenReturn(Optional.empty());

        Optional<Video> result = videoService.getVideo(testVideoId);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should get videos by user")
    void shouldGetVideosByUser() {
        Page<Video> videoPage = new PageImpl<>(List.of(testVideo));
        when(videoRepository.findByUserUuid(eq(testUserId), any(Pageable.class)))
                .thenReturn(videoPage);

        Page<Video> result = videoService.getVideosByUser(testUserId, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals(testVideo.getTitle(), result.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("Should get active videos")
    void shouldGetActiveVideos() {
        Page<Video> videoPage = new PageImpl<>(List.of(testVideo));
        when(videoRepository.findByStatus(eq(VideoStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(videoPage);

        Page<Video> result = videoService.getVideos(PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals(VideoStatus.ACTIVE, result.getContent().get(0).getStatus());
    }

    @Test
    @DisplayName("Should get trending videos")
    void shouldGetTrendingVideos() {
        Page<Video> videoPage = new PageImpl<>(List.of(testVideo));
        when(videoRepository.findTrendingVideos(any(Pageable.class))).thenReturn(videoPage);

        Page<Video> result = videoService.getTrendingVideos(PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        verify(videoRepository).findTrendingVideos(any(Pageable.class));
    }

    @Test
    @DisplayName("Should get latest videos")
    void shouldGetLatestVideos() {
        Page<Video> videoPage = new PageImpl<>(List.of(testVideo));
        when(videoRepository.findLatestVideos(any(Pageable.class))).thenReturn(videoPage);

        Page<Video> result = videoService.getLatestVideos(PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        verify(videoRepository).findLatestVideos(any(Pageable.class));
    }

    @Test
    @DisplayName("Should delete video successfully")
    void shouldDeleteVideo() {
        when(videoRepository.findById(testVideoId)).thenReturn(Optional.of(testVideo));
        doNothing().when(storageService).deleteVideo(anyString());
        doNothing().when(storageService).deleteThumbnail(anyString());
        when(videoRepository.save(any(Video.class))).thenReturn(testVideo);

        boolean result = videoService.deleteVideo(testVideoId, testUserId);

        assertTrue(result);
        verify(storageService).deleteVideo(testVideo.getFilePath());
        verify(storageService).deleteThumbnail(testVideo.getThumbnailPath());
        verify(videoRepository).save(testVideo);
        assertEquals(VideoStatus.DELETED, testVideo.getStatus());
    }

    @Test
    @DisplayName("Should not delete video when user is not owner")
    void shouldNotDeleteVideoWhenUserIsNotOwner() {
        UUID differentUserId = UUID.randomUUID();
        when(videoRepository.findById(testVideoId)).thenReturn(Optional.of(testVideo));

        boolean result = videoService.deleteVideo(testVideoId, differentUserId);

        assertFalse(result);
        verify(storageService, never()).deleteVideo(anyString());
        verify(videoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should not delete video when video not found")
    void shouldNotDeleteVideoWhenNotFound() {
        when(videoRepository.findById(testVideoId)).thenReturn(Optional.empty());

        boolean result = videoService.deleteVideo(testVideoId, testUserId);

        assertFalse(result);
        verify(storageService, never()).deleteVideo(anyString());
    }

    @Test
    @DisplayName("Should mark video as viewed")
    void shouldMarkVideoAsViewed() {
        when(viewedVideoRepository.existsByUserUuidAndVideoUuid(testUserId, testVideoId)).thenReturn(false);
        when(viewedVideoRepository.save(any(ViewedVideo.class))).thenReturn(new ViewedVideo(testUserId, testVideoId));

        videoService.markAsViewed(testUserId, testVideoId);

        verify(viewedVideoRepository).save(any(ViewedVideo.class));
    }

    @Test
    @DisplayName("Should not mark video as viewed when already viewed")
    void shouldNotMarkVideoAsViewedWhenAlreadyViewed() {
        when(viewedVideoRepository.existsByUserUuidAndVideoUuid(testUserId, testVideoId)).thenReturn(true);

        videoService.markAsViewed(testUserId, testVideoId);

        verify(viewedVideoRepository, never()).save(any(ViewedVideo.class));
    }

    @Test
    @DisplayName("Should increment view count")
    void shouldIncrementViewCount() {
        int initialViews = testVideo.getViews();
        when(videoRepository.save(any(Video.class))).thenAnswer(invocation -> invocation.getArgument(0));

        videoService.incrementViews(testVideo);

        assertEquals(initialViews + 1, testVideo.getViews());
        verify(videoRepository).save(testVideo);
    }

    @Test
    @DisplayName("Should get videos by quest")
    void shouldGetVideosByQuest() {
        UUID questUuid = UUID.randomUUID();
        Page<Video> videoPage = new PageImpl<>(List.of(testVideo));
        when(videoRepository.findByQuestUuid(eq(questUuid), any(Pageable.class)))
                .thenReturn(videoPage);

        Page<Video> result = videoService.getVideosByQuest(questUuid, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        verify(videoRepository).findByQuestUuid(eq(questUuid), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get next unviewed video for user")
    void shouldGetNextUnviewedVideo() {
        when(videoRepository.findUnviewedVideosByUserUuid(eq(testUserId), any(PageRequest.class)))
                .thenReturn(List.of(testVideo));
        when(viewedVideoRepository.existsByUserUuidAndVideoUuid(testUserId, testVideoId)).thenReturn(false);
        when(viewedVideoRepository.save(any(ViewedVideo.class))).thenReturn(new ViewedVideo(testUserId, testVideoId));
        when(videoRepository.save(any(Video.class))).thenReturn(testVideo);

        Optional<Video> result = videoService.getNextVideo(testUserId);

        assertTrue(result.isPresent());
        assertEquals(testVideo, result.get());
    }

    @Test
    @DisplayName("Should get random video when no unviewed videos")
    void shouldGetRandomVideoWhenNoUnviewedVideos() {
        when(videoRepository.findUnviewedVideosByUserUuid(eq(testUserId), any(PageRequest.class)))
                .thenReturn(List.of());
        when(videoRepository.findRandomVideo()).thenReturn(Optional.of(testVideo));
        when(viewedVideoRepository.existsByUserUuidAndVideoUuid(testUserId, testVideoId)).thenReturn(false);
        when(viewedVideoRepository.save(any(ViewedVideo.class))).thenReturn(new ViewedVideo(testUserId, testVideoId));

        Optional<Video> result = videoService.getNextVideo(testUserId);

        assertTrue(result.isPresent());
        verify(videoRepository).findRandomVideo();
    }
}
