package com.kidsstory.dto;

public record TokenResponse(String accessToken, String tokenType) {

    public TokenResponse(String accessToken) {
        this(accessToken, "bearer");
    }
}
