package com.dayquest.dayquestbackend.video;

public class VideoDTO {
    private String title;
    private String description;
    private byte[] thumbnail;

    public VideoDTO(String title, String description, byte[] thumbnail) {
        this.title = title;
        this.description = description;
        this.thumbnail = thumbnail;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

}
