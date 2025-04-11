package com.dayquest.dayquestbackend.hashtag;

import com.dayquest.dayquestbackend.quest.QuestRepository;
import com.dayquest.dayquestbackend.video.dto.VideoDTO;
import com.dayquest.dayquestbackend.video.repository.VideoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class HashtagService {
    private final HashtagRepository hashtagRepository;
    private final VideoRepository videoRepository;
    private final QuestRepository questRepository;

    public HashtagService(HashtagRepository hashtagRepository, VideoRepository videoRepository, QuestRepository questRepository) {
        this.hashtagRepository = hashtagRepository;
        this.videoRepository = videoRepository;
        this.questRepository = questRepository;
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

    CompletableFuture<List<Hashtag>> searchPaginatedHashtags(int page, int size, String query) {
        return CompletableFuture.supplyAsync(() -> {
            Sort sort = Sort.by(Sort.Direction.DESC, "videoCount");
            return hashtagRepository.findHashtagsByHashtagContainingIgnoreCase(query, PageRequest.of(page, size, sort)).getContent();
        });
    }

    CompletableFuture<List<VideoDTO>> getPaginatedVideosWithHashtag(int page, int size, String hashtag) {
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
}
