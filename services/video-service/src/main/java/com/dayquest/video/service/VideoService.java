package com.dayquest.video.service;

import com.dayquest.common.exception.ResourceNotFoundException;
import com.dayquest.video.dto.UploadVideoDTO;
import com.dayquest.video.dto.VideoResponseDTO;
import com.dayquest.video.model.SecurityLevel;
import com.dayquest.video.model.Video;
import com.dayquest.video.model.VideoStatus;
import com.dayquest.video.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VideoService {

    private static final Logger logger = LoggerFactory.getLogger(VideoService.class);

    private final VideoRepository videoRepository;

    @Value("${video.upload.path:/opt/dayquestcdn/videos/unprocessed}")
    private String uploadPath;

    @Value("${video.processed.path:/opt/dayquestcdn/videos/processed}")
    private String processedPath;

    public VideoService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @Cacheable(value = "videos", key = "#uuid")
    public VideoResponseDTO getVideoById(UUID uuid) {
        Video video = videoRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found with id: " + uuid));
        return new VideoResponseDTO(video);
    }

    public Page<VideoResponseDTO> getAllVideos(int page, int size) {
        Page<Video> videos = videoRepository.findByStatusAndSecurityLevelNot(
                VideoStatus.READY, SecurityLevel.NSFW, PageRequest.of(page, size));
        return videos.map(VideoResponseDTO::new);
    }

    @Transactional
    @CacheEvict(value = "videos", allEntries = true)
    public VideoResponseDTO uploadVideo(UploadVideoDTO dto, UUID userUuid, MultipartFile file) throws IOException {
        Video video = new Video(dto.getTitle(), dto.getDescription(), userUuid, dto.getQuestUuid());
        video.setStatus(VideoStatus.PENDING);
        video = videoRepository.save(video);

        // Save file
        String fileName = video.getUuid() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadPath, fileName);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, file.getBytes());
        
        video.setFilePath(filePath.toString());
        video = videoRepository.save(video);

        // TODO: Trigger video processing asynchronously
        logger.info("Video uploaded: {} by user {}", video.getUuid(), userUuid);

        return new VideoResponseDTO(video);
    }

    @Transactional
    @CacheEvict(value = "videos", key = "#uuid")
    public void deleteVideo(UUID uuid) {
        Video video = videoRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
        video.setStatus(VideoStatus.DELETED);
        videoRepository.save(video);
    }

    @Transactional
    @CacheEvict(value = "videos", key = "#uuid")
    public void upvoteVideo(UUID uuid) {
        Video video = videoRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
        video.setUpVotes(video.getUpVotes() + 1);
        videoRepository.save(video);
    }

    @Transactional
    @CacheEvict(value = "videos", key = "#uuid")
    public void downvoteVideo(UUID uuid) {
        Video video = videoRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
        video.setDownVotes(video.getDownVotes() + 1);
        videoRepository.save(video);
    }

    @Transactional
    @CacheEvict(value = "videos", key = "#uuid")
    public void incrementViews(UUID uuid) {
        Video video = videoRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
        video.setViews(video.getViews() + 1);
        videoRepository.save(video);
    }

    public Page<VideoResponseDTO> getVideosByUser(UUID userUuid, int page, int size) {
        Page<Video> videos = videoRepository.findByUserUuid(userUuid, PageRequest.of(page, size));
        return videos.map(VideoResponseDTO::new);
    }

    public Page<VideoResponseDTO> getVideosByQuest(UUID questUuid, int page, int size) {
        Page<Video> videos = videoRepository.findByQuestUuid(questUuid, PageRequest.of(page, size));
        return videos.map(VideoResponseDTO::new);
    }

    public List<VideoResponseDTO> getRecentVideos(int limit) {
        List<Video> videos = videoRepository.findRecentVideos(VideoStatus.READY, PageRequest.of(0, limit));
        return videos.stream()
                .map(VideoResponseDTO::new)
                .collect(Collectors.toList());
    }

    public List<VideoResponseDTO> getTopRatedVideos(int limit) {
        List<Video> videos = videoRepository.findTopRatedVideos(VideoStatus.READY, PageRequest.of(0, limit));
        return videos.stream()
                .map(VideoResponseDTO::new)
                .collect(Collectors.toList());
    }

    public Page<VideoResponseDTO> searchVideos(String query, int page, int size) {
        Page<Video> videos = videoRepository.findByTitleContainingIgnoreCaseAndStatus(
                query, VideoStatus.READY, PageRequest.of(page, size));
        return videos.map(VideoResponseDTO::new);
    }

    @Transactional
    @CacheEvict(value = "videos", key = "#uuid")
    public void incrementComments(UUID uuid) {
        Video video = videoRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
        video.setComments(video.getComments() + 1);
        videoRepository.save(video);
    }
}
