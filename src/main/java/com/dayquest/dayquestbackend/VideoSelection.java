package com.dayquest.dayquestbackend;

import com.dayquest.dayquestbackend.video.Video;
import com.dayquest.dayquestbackend.video.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class VideoSelection {

    private final VideoRepository videoRepository;

    @Autowired
    public VideoSelection(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    public static int nextVideo(Video[] videos, String[] hashtags, int percentage) {
        if (videos.length == 0) {
            System.out.println("No videos available.");
            return 0;
        }

        Random random = new Random();
        int videoIndex = random.nextInt(videos.length);
        System.out.println("Random video index: " + videoIndex);

        if (hashtags.length == 0 || random.nextInt(101) <= percentage) return videoIndex;

        System.out.println("Not a random video.");
        String randomHashtag = hashtags[random.nextInt(hashtags.length)];
        System.out.println("Random hashtag: " + randomHashtag);
        return indexOfHashtag(videos, randomHashtag);
    }


    private static int indexOfHashtag(Video[] arr, String element) {
        Random random = new Random();
        int[] indices = {};

        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[i].getHashtags().length; j++) {
                if (arr[i].getHashtags()[j] == element) indices = add(indices, i);
            }
        }


        if (indices.length != 0) return indices[random.nextInt(indices.length)];
        System.out.println("Video with this hashtag does not exsist.");
        return -1;
    }

    private static int[] add(int[] arr, int element) {
        int newarr[] = new int[arr.length + 1];

        for (int i = 0; i < arr.length; i++) {
            newarr[i] = arr[i];
        }

        newarr[arr.length] = element;
        return newarr;
    }

}
