package com.dayquest.dayquestbackend.friends;

public class FriendDTO {
    private String username;

    public FriendDTO(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }


    public void setUsername(String username) {
        this.username = username;
    }
}
