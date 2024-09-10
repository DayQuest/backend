package com.example.dayquest.dto;

public class FriendDTO {
    private String username;

    // Constructor
    public FriendDTO(String username) {
        this.username = username;
    }

    // Getter
    public String getUsername() {
        return username;
    }

    // Setter (optional, depending on your use case)
    public void setUsername(String username) {
        this.username = username;
    }
}
