package com.medirandevu.auth.controller;

import com.medirandevu.auth.dto.*;
import com.medirandevu.auth.service.classes.CookieService;
import com.medirandevu.auth.service.interfaces.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    public final AuthService authService;
    private final CookieService cookieService;

    // Only reachable with a token whose role is ADMIN (ROLE_ADMIN authority set by JwtAuthFilter);
    // other authenticated users get 403 from GlobalExceptionHandler.
    @PostMapping("/doctors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createDoctorAccount(
            @Valid @RequestBody CreateDoctorAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.createDoctorAccount(request));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request){
        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request){
        LoginResult result = authService.login(request);
        ResponseCookie cookie = cookieService.createAccessCookie(result.token());

        // The JWT travels only in the HttpOnly Set-Cookie header; the body contains user info, not the token.
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result.user());

    }
    // Stateless logout: tell the browser to drop the cookie. There is no server-side
    // revocation, so a copied token stays valid until it expires.
    @PostMapping("/logout")
    public ResponseEntity<Void> logOut(){
        ResponseCookie cookie = cookieService.clearAccessCookie();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        // JwtAuthFilter sets the principal name to the token's "sub" claim, which is the user id.
        Long userId = Long.valueOf(authentication.getName());
        return ResponseEntity.ok(authService.getCurrentUser(userId));
    }

}
