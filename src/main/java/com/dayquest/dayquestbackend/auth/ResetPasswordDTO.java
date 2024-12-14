package com.dayquest.dayquestbackend.auth;

public class ResetPasswordDTO {
    private String email;
    private String password;
    private String token;

    public ResetPasswordDTO() {
    }

    public ResetPasswordDTO(String password, String token) {
        this.password = password;
        this.token = token;
    }

    public String getPassword() {
        return password;
    }

    public String getToken() {
        return token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
