package com.medirandevu.auth.service.interfaces;

import com.medirandevu.auth.dto.LoginRequest;
import com.medirandevu.auth.dto.LoginResult;
import com.medirandevu.auth.dto.RegisterRequest;
import com.medirandevu.auth.dto.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    LoginResult login(LoginRequest request);
    UserResponse getCurrentUser(Long userId);
}
