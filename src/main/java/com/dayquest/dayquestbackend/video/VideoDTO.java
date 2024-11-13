package com.dayquest.dayquestbackend.video;

public class VideoDTO {
    private String title;
    private String description;
    private Long upVotes;
    private Long downVotes;
    private String username;
    private String filePath;
    private String thumbnail;

    // Constructor
    public VideoDTO(
            String title,
            String description,
            long upVotes,
            long downVotes,
            String username,
            String filePath,
            String thumbnail) {
        this.title = title;
        this.description = description;
        this.upVotes = upVotes;
        this.downVotes = downVotes;
        this.username = username;
        this.filePath = filePath;
        this.thumbnail = thumbnail;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Long getUpVotes() { return upVotes; }
    public Long getDownVotes() { return downVotes; }
    public String getUsername() { return username; }
    public String getFilePath() { return filePath; }
    public String getThumbnail() { return thumbnail; }
}
