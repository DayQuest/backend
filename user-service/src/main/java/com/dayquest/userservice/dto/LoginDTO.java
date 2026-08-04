package com.dayquest.userservice.dto;


import jakarta.validation.constraints.NotBlank;


@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
public class LoginDTO {
    @NotBlank
    private String username;
    private String password;

    public @NotBlank String getUsername() {
        return username;
    }






}
