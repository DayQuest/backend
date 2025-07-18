package com.dayquest.userservice.dto;


import jakarta.validation.constraints.NotBlank;

public class LoginDTO {
    @NotBlank
    private String username;
    private String password;

    public @NotBlank String getUsername() {
        return username;
    }

    public void setUsername(@NotBlank String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
