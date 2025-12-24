package com.dayquest.user.dto;

/**
 * DTO for updating user profile.
 */
public class UpdateUserDTO {
    private String username;
    private String email;

    public UpdateUserDTO() {}

    public UpdateUserDTO(String username, String email) {
        this.username = username;
        this.email = email;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
