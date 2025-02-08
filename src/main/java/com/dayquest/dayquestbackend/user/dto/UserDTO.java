package com.dayquest.dayquestbackend.user.dto;

public class UserDTO {
    private String username;
    private String email;
    private String password;
    private String betaKey;

    // Constructor with correct parameters
    public UserDTO(String username, String email, String password, String betaKey) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.betaKey = betaKey;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBetaKey() {
        return betaKey;
    }

    public void setBetaKey(String betaKey) {
        this.betaKey = betaKey;
    }
}
