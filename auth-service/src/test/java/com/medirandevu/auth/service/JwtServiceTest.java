package com.medirandevu.auth.service;

import com.medirandevu.auth.entity.User;
import com.medirandevu.auth.security.JwtProperties;
import com.medirandevu.auth.service.classes.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = base64("0123456789abcdef0123456789abcdef"); // 32 byte
    private static final String OTHER_SECRET = base64("fedcba9876543210fedcba9876543210");

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(props(SECRET, 60_000));
        user = User.builder()
                .id(42L)
                .email("hasta@example.com")
                .passwordHash("hash")
                .role(User.Role.DOCTOR)
                .build();
    }

    @Test
    void constructor_rejectsSecretShorterThan256Bits() {
        JwtProperties shortSecret = props(base64("too-short-secret"), 60_000);

        assertThatThrownBy(() -> new JwtService(shortSecret))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256 bit");
    }

    @Test
    void generateToken_thenParse_returnsSubjectEmailAndRole() {
        String token = jwtService.generateToken(user);

        Claims claims = jwtService.parse(token);

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("hasta@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("DOCTOR");
    }

    @Test
    void generateToken_setsExpirationFromProperties() {
        Claims claims = jwtService.parse(jwtService.generateToken(user));

        long lifetime = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(lifetime).isEqualTo(60_000);
    }

    @Test
    void parse_rejectsExpiredToken() throws InterruptedException {
        JwtService shortLived = new JwtService(props(SECRET, 1));
        String token = shortLived.generateToken(user);
        Thread.sleep(1_100); // JWT tarihleri saniye hassasiyetinde

        assertThatThrownBy(() -> shortLived.parse(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void parse_rejectsTokenSignedWithDifferentKey() {
        String foreignToken = new JwtService(props(OTHER_SECRET, 60_000)).generateToken(user);

        assertThatThrownBy(() -> jwtService.parse(foreignToken))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void parse_rejectsTamperedPayload() {
        String[] parts = jwtService.generateToken(user).split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"1\",\"role\":\"ADMIN\"}".getBytes());
        String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThatThrownBy(() -> jwtService.parse(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parse_rejectsGarbage() {
        assertThatThrownBy(() -> jwtService.parse("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }

    private static JwtProperties props(String secret, long expirationMs) {
        return new JwtProperties(secret, expirationMs, "access_token", false);
    }

    private static String base64(String raw) {
        return Base64.getEncoder().encodeToString(raw.getBytes());
    }
}
