package com.medirandevu.auth.service;

import com.medirandevu.auth.security.JwtProperties;
import com.medirandevu.auth.service.classes.CookieService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CookieServiceTest {

    private final CookieService cookieService =
            new CookieService(new JwtProperties("secret", 900_000, "access_token", true));

    @Test
    void createAccessCookie_hasSecureAttributesAndTokenLifetime() {
        ResponseCookie cookie = cookieService.createAccessCookie("jwt-value");

        assertThat(cookie.getName()).isEqualTo("access_token");
        assertThat(cookie.getValue()).isEqualTo("jwt-value");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofMillis(900_000));
    }

    @Test
    void createAccessCookie_respectsCookieSecureFalse() {
        CookieService insecure = new CookieService(new JwtProperties("secret", 1000, "access_token", false));

        assertThat(insecure.createAccessCookie("t").isSecure()).isFalse();
    }

    @Test
    void clearAccessCookie_isEmptyAndExpiresImmediately() {
        ResponseCookie cookie = cookieService.clearAccessCookie();

        assertThat(cookie.getName()).isEqualTo("access_token");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
    }

    @Test
    void extractToken_returnsNullWhenNoCookies() {
        assertThat(cookieService.extractToken(new MockHttpServletRequest())).isNull();
    }

    @Test
    void extractToken_returnsNullWhenCookieNameDoesNotMatch() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("other", "x"));

        assertThat(cookieService.extractToken(request)).isNull();
    }

    @Test
    void extractToken_returnsValueOfMatchingCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("other", "x"), new Cookie("access_token", "jwt-value"));

        assertThat(cookieService.extractToken(request)).isEqualTo("jwt-value");
    }
}
