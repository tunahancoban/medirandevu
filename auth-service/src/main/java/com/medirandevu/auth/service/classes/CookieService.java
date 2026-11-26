package com.medirandevu.auth.service.classes;

import com.medirandevu.auth.security.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class CookieService  {
    private final JwtProperties props;

    public ResponseCookie createAccessCookie(String token){
        return ResponseCookie.from(props.cookieName(), token)
                .httpOnly(true)
                .secure(props.cookieSecure())
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofMillis(props.expirationMs()))
                .build();

    }

    public ResponseCookie clearAccessCookie(){
        return ResponseCookie.from(props.cookieName(), "")
                .httpOnly(true)
                .secure(props.cookieSecure())
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
    }

    public String extractToken(HttpServletRequest request){
        if(request.getCookies()==null) return null;
        for(Cookie cookie : request.getCookies()) {
            if(props.cookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
