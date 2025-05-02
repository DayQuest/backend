package com.dayquest.dayquestbackend.hashtag;

import com.dayquest.dayquestbackend.quest.QuestRepository;
import com.dayquest.dayquestbackend.user.ids.UserLikedHashtagId;
import com.dayquest.dayquestbackend.user.models.User;
import com.dayquest.dayquestbackend.user.models.UserLikedHashtag;
import com.dayquest.dayquestbackend.user.repositories.UserLikedHashtagRepository;
import com.dayquest.dayquestbackend.video.dto.VideoDTO;
import com.dayquest.dayquestbackend.video.repository.VideoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class HashtagService {
    private final HashtagRepository hashtagRepository;
    private final VideoRepository videoRepository;
    private final QuestRepository questRepository;
    private final UserLikedHashtagRepository userLikedHashtagRepository;

    public HashtagService(HashtagRepository hashtagRepository, VideoRepository videoRepository, QuestRepository questRepository, UserLikedHashtagRepository userLikedHashtagRepository) {
        this.hashtagRepository = hashtagRepository;
        this.videoRepository = videoRepository;
        this.questRepository = questRepository;
        this.userLikedHashtagRepository = userLikedHashtagRepository;
    }

    public CompletableFuture<Hashtag> createHashtag(String hashtag) {
        return CompletableFuture.supplyAsync(() -> {
            Hashtag newHashtag = new Hashtag();
            newHashtag.setHashtag(hashtag);
            newHashtag.setVideoCount(0);
            hashtagRepository.save(newHashtag);
            return newHashtag;
        });
    }

    public CompletableFuture<List<Hashtag>> searchPaginatedHashtags(int page, int size, String query) {
        return CompletableFuture.supplyAsync(() -> {
            Sort sort = Sort.by(Sort.Direction.DESC, "videoCount");
            return hashtagRepository.findHashtagsByHashtagContainingIgnoreCase(query, PageRequest.of(page, size, sort)).getContent();
        });
    }

    public CompletableFuture<List<VideoDTO>> getPaginatedVideosWithHashtag(int page, int size, String hashtag) {
        return CompletableFuture.supplyAsync(() -> {
            Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
            return videoRepository.findVideosByHashtagsContainsIgnoreCase(hashtag, PageRequest.of(page, size)).stream().map(
                    video -> {
                        return new VideoDTO(video.getTitle(),
                                video.getDescription(),
                                video.getUpVotes(),
                                video.getDownVotes(),
                                video.getUser().getUsername(),
                                video.getFilePath(),
                                null,
                                questRepository.findByUuid(video.getQuestUuid()),
                                video.getUuid(),
                                video.getCreatedAt(),
                                false);
                    }
            ).toList();
        });
    }

    public CompletableFuture<Boolean> likeHashtags(List<Hashtag> hashtags, User user) {
        return CompletableFuture.supplyAsync(() -> {
            for (Hashtag hashtag : hashtags) {
                UserLikedHashtagId userLikedHashtagId = new UserLikedHashtagId(user.getUuid(), hashtag.getUuid());
                Optional<UserLikedHashtag> userLikedHashtagOptional = userLikedHashtagRepository.findById(userLikedHashtagId);
                if (userLikedHashtagOptional.isPresent()) {
                    UserLikedHashtag userLikedHashtag = userLikedHashtagOptional.get();
                    userLikedHashtag.setLastLikedTimestamp(LocalDateTime.now());
                }

                UserLikedHashtag userLikedHashtag = new UserLikedHashtag(user, hashtag);
                userLikedHashtag.setLastLikedTimestamp(LocalDateTime.now());
                userLikedHashtagRepository.save(userLikedHashtag);
            }
            return true;
        });
    }

}
