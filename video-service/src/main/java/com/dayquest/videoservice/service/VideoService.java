package com.dayquest.videoservice.service;

import com.dayquest.videoservice.dto.VideoDTO;
import com.dayquest.videoservice.dto.VideoUploadRequest;
import com.dayquest.videoservice.model.*;
import com.dayquest.videoservice.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

@Service
public class VideoService {

    private static final Logger logger = LoggerFactory.getLogger(VideoService.class);

    private final VideoRepository videoRepository;
    private final ViewedVideoRepository viewedVideoRepository;
    private final VideoRatingRepository videoRatingRepository;
    private final StorageService storageService;

    public VideoService(
            VideoRepository videoRepository,
            ViewedVideoRepository viewedVideoRepository,
            VideoRatingRepository videoRatingRepository,
            StorageService storageService) {
        this.videoRepository = videoRepository;
        this.viewedVideoRepository = viewedVideoRepository;
        this.videoRatingRepository = videoRatingRepository;
        this.storageService = storageService;
    }

    @Async
    @Transactional
    public CompletableFuture<Video> uploadVideo(MultipartFile file, VideoUploadRequest request, UUID userUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Video video = new Video();
            video.setTitle(request.getTitle());
            video.setDescription(request.getDescription());
            video.setUserUuid(userUuid);
            video.setQuestUuid(request.getQuestUuid());
            video.setStatus(VideoStatus.PROCESSING);
            // Set temporary file path to satisfy database constraint before we have the real ID/path
            video.setFilePath("pending_upload");

            video = videoRepository.save(video);

            try {
                String filePath = storageService.uploadVideo(file, video.getUuid());
                video.setFilePath(filePath);
                video.setStatus(VideoStatus.ACTIVE);
                logger.info("Video uploaded successfully: {}", video.getUuid());
            } catch (Exception e) {
                video.setStatus(VideoStatus.DELETED);
                logger.error("Failed to upload video: {}", video.getUuid(), e);
            }

            return videoRepository.save(video);
        });
    }

    public Optional<Video> getVideo(UUID videoUuid) {
        return videoRepository.findById(videoUuid);
    }

    public Page<Video> getVideos(UUID userUuid, UUID questUuid, Pageable pageable) {
        Specification<Video> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Default filter: status ACTIVE and deleted false
            predicates.add(criteriaBuilder.equal(root.get("status"), VideoStatus.ACTIVE));
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            if (userUuid != null) {
                predicates.add(criteriaBuilder.equal(root.get("userUuid"), userUuid));
            }
            if (questUuid != null) {
                predicates.add(criteriaBuilder.equal(root.get("questUuid"), questUuid));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return videoRepository.findAll(spec, pageable);
    }

    public Page<Video> getVideos(Pageable pageable) {
        return videoRepository.findByStatus(VideoStatus.ACTIVE, pageable);
    }

    public Page<Video> getVideosByUser(UUID userUuid, Pageable pageable) {
        return videoRepository.findByUserUuid(userUuid, pageable);
    }

    public Page<Video> getVideosByQuest(UUID questUuid, Pageable pageable) {
        return videoRepository.findByQuestUuid(questUuid, pageable);
    }

    public Page<Video> getTrendingVideos(Pageable pageable) {
        return videoRepository.findTrendingVideos(pageable);
    }

    public Page<Video> getLatestVideos(Pageable pageable) {
        return videoRepository.findLatestVideos(pageable);
    }

    @Transactional
    public Optional<Video> getNextVideo(UUID userUuid) {
        List<Video> unviewedVideos = videoRepository.findUnviewedVideosByUserUuid(
                userUuid, PageRequest.of(0, 1));

        if (unviewedVideos.isEmpty()) {
            Optional<Video> randomVideo = videoRepository.findRandomVideo();
            randomVideo.ifPresent(video -> markAsViewed(userUuid, video.getUuid()));
            return randomVideo;
        }

        Video video = unviewedVideos.get(0);
        markAsViewed(userUuid, video.getUuid());
        incrementViews(video);
        return Optional.of(video);
    }

    @Transactional
    public void markAsViewed(UUID userUuid, UUID videoUuid) {
        if (!viewedVideoRepository.existsByUserUuidAndVideoUuid(userUuid, videoUuid)) {
            viewedVideoRepository.save(new ViewedVideo(userUuid, videoUuid));
        }
    }

    //TODO: make this Atomic
    @Transactional
    public void incrementViews(Video video) {
        video.setViews(video.getViews() + 1);
        videoRepository.save(video);
    }

    @Transactional
    public boolean deleteVideo(UUID videoUuid, UUID userUuid) {
        Optional<Video> videoOpt = videoRepository.findById(videoUuid);
        if (videoOpt.isEmpty()) {
            return false;
        }

        Video video = videoOpt.get();
        if (!video.getUserUuid().equals(userUuid)) {
            return false;
        }

        storageService.deleteVideo(video.getFilePath());
        if (video.getThumbnailPath() != null) {
            storageService.deleteThumbnail(video.getThumbnailPath());
        }

        video.setStatus(VideoStatus.DELETED);
        videoRepository.save(video);
        return true;
    }

    @Transactional
    public boolean upvoteVideo(UUID videoUuid, UUID userUuid) {
        Optional<Video> videoOpt = videoRepository.findById(videoUuid);
        if (videoOpt.isEmpty()) {
            return false;
        }

        Video video = videoOpt.get();
        Optional<VideoRating> existingRating = videoRatingRepository.findByUserUuidAndVideoUuid(userUuid, videoUuid);

        if (existingRating.isPresent()) {
            VideoRating rating = existingRating.get();
            if (rating.isUpvote()) {
                // Already upvoted - remove vote
                videoRatingRepository.delete(rating);
                video.setUpVotes(video.getUpVotes() - 1);
            } else {
                // Change from downvote to upvote
                rating.setUpvote(true);
                videoRatingRepository.save(rating);
                video.setDownVotes(video.getDownVotes() - 1);
                video.setUpVotes(video.getUpVotes() + 1);
            }
        } else {
            // New upvote
            videoRatingRepository.save(new VideoRating(userUuid, videoUuid, true));
            video.setUpVotes(video.getUpVotes() + 1);
        }

        videoRepository.save(video);
        return true;
    }

    @Transactional
    public boolean downvoteVideo(UUID videoUuid, UUID userUuid) {
        Optional<Video> videoOpt = videoRepository.findById(videoUuid);
        if (videoOpt.isEmpty()) {
            return false;
        }

        Video video = videoOpt.get();
        Optional<VideoRating> existingRating = videoRatingRepository.findByUserUuidAndVideoUuid(userUuid, videoUuid);

        if (existingRating.isPresent()) {
            VideoRating rating = existingRating.get();
            if (!rating.isUpvote()) {
                // Already downvoted - remove vote
                videoRatingRepository.delete(rating);
                video.setDownVotes(video.getDownVotes() - 1);
            } else {
                // Change from upvote to downvote
                rating.setUpvote(false);
                videoRatingRepository.save(rating);
                video.setUpVotes(video.getUpVotes() - 1);
                video.setDownVotes(video.getDownVotes() + 1);
            }
        } else {
            // New downvote
            videoRatingRepository.save(new VideoRating(userUuid, videoUuid, false));
            video.setDownVotes(video.getDownVotes() + 1);
        }

        videoRepository.save(video);
        return true;
    }

    public VideoDTO toDTO(Video video, UUID currentUserUuid) {
        VideoDTO dto = new VideoDTO();
        dto.setUuid(video.getUuid());
        dto.setTitle(video.getTitle());
        dto.setDescription(video.getDescription());
        dto.setUserUuid(video.getUserUuid());
        dto.setQuestUuid(video.getQuestUuid());
        dto.setUpVotes(video.getUpVotes());
        dto.setDownVotes(video.getDownVotes());
        dto.setViews(video.getViews());
        dto.setComments(video.getComments());
        dto.setStatus(video.getStatus());
        dto.setSecurityLevel(video.getSecurityLevel());
        dto.setDuration(video.getDuration());
        dto.setCreatedAt(video.getCreatedAt());

        try {
            dto.setVideoUrl(storageService.getVideoUrl(video.getFilePath()));
            if (video.getThumbnailPath() != null) {
                dto.setThumbnailUrl(storageService.getThumbnailUrl(video.getThumbnailPath()));
            }
        } catch (Exception e) {
            logger.error("Failed to get URLs for video: {}", video.getUuid(), e);
        }

        if (currentUserUuid != null) {
            Optional<VideoRating> rating = videoRatingRepository.findByUserUuidAndVideoUuid(currentUserUuid, video.getUuid());
            if (rating.isPresent()) {
                dto.setLikedByUser(rating.get().isUpvote());
                dto.setDislikedByUser(!rating.get().isUpvote());
            }
        }

        return dto;
    }
}

