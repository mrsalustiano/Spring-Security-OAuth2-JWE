package com.example.oauth2.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@NoArgsConstructor
public class TokenRequest {
    @NotBlank(message = "Grant type is required")
    private String grant_type;

    private String username;
    private String password;
    private String refresh_token;
    private String client_id;
    private String client_secret;
    private String scope;


}