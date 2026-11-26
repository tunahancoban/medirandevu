package com.medirandevu.auth.service.interfaces;

import com.medirandevu.auth.dto.*;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    LoginResult login(LoginRequest request);
    UserResponse getCurrentUser(Long userId);
    UserResponse createDoctorAccount(CreateDoctorAccountRequest request);
}
