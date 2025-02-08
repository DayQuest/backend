package com.dayquest.dayquestbackend.hashtag;

public class CreateHashtagDTO {
    private String hashtag;

    public CreateHashtagDTO(String hashtag) {
        this.hashtag = hashtag;
    }

    public String getHashtag() {
        return hashtag;
    }
}
