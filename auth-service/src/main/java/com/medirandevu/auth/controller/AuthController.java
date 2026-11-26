package com.medirandevu.auth.controller;

import com.medirandevu.auth.dto.LoginRequest;
import com.medirandevu.auth.dto.LoginResult;
import com.medirandevu.auth.dto.RegisterRequest;
import com.medirandevu.auth.dto.UserResponse;
import com.medirandevu.auth.service.classes.CookieService;
import com.medirandevu.auth.service.interfaces.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    public final AuthService authService;
    private final CookieService cookieService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request){
        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request){
        LoginResult result = authService.login(request);
        ResponseCookie cookie = cookieService.createAccessCookie(result.token());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result.user());

    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logOut(){
        ResponseCookie cookie = cookieService.clearAccessCookie();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        return ResponseEntity.ok(authService.getCurrentUser(userId));
    }

}
