package com.dayquest.dayquestbackend.friends;


public class FriendDTO {
    private final String username;

    public FriendDTO(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
