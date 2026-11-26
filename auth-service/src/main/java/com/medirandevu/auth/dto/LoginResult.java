package com.medirandevu.auth.dto;

public record LoginResult(String token, UserResponse user) {
}
