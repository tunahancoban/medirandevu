package com.medirandevu.auth.service.classes;

import com.medirandevu.auth.security.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Creates, clears and reads the cookie that carries the access token to the browser.
 *
 * <p>Keeping the JWT in a cookie (instead of returning it in the response body for the
 * frontend to store in localStorage) means JavaScript never sees the token.
 */
@Component
@RequiredArgsConstructor
public class CookieService  {
    private final JwtProperties props;

    /**
     * Wraps a freshly issued JWT in a {@code Set-Cookie} value.
     */
    public ResponseCookie createAccessCookie(String token){
        return ResponseCookie.from(props.cookieName(), token)
                // Not readable from document.cookie, which blocks token theft via XSS.
                .httpOnly(true)
                // Sent only over HTTPS when enabled; kept configurable so local HTTP dev works.
                // Must be true in production.
                .secure(props.cookieSecure())
                // Browser won't attach the cookie to requests initiated by other sites,
                // which is the main CSRF protection since CSRF tokens are disabled.
                .sameSite("Strict")
                // Sent with every path of this host, not only /api/auth.
                .path("/")
                // Cookie lives exactly as long as the JWT inside it.
                .maxAge(Duration.ofMillis(props.expirationMs()))
                .build();

    }

    /**
     * Returns a cookie that makes the browser delete the access cookie (logout).
     *
     * <p>Browsers only overwrite a cookie whose name, path (and domain) match, so the
     * attributes mirror {@link #createAccessCookie}. {@code Max-Age=0} expires it immediately.
     * Note this only removes the browser copy: the JWT itself stays valid until it expires.
     */
    public ResponseCookie clearAccessCookie(){
        return ResponseCookie.from(props.cookieName(), "")
                .httpOnly(true)
                .secure(props.cookieSecure())
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
    }

    /**
     * Reads the raw token from the incoming request's cookies, or {@code null} if absent.
     * No validation happens here; that is {@link JwtService#parse}'s job.
     */
    public String extractToken(HttpServletRequest request){
        // getCookies() returns null (not an empty array) when the request has no cookies.
        if(request.getCookies()==null) return null;
        for(Cookie cookie : request.getCookies()) {
            if(props.cookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
