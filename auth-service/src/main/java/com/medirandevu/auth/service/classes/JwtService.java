package com.medirandevu.auth.service.classes;

import com.medirandevu.auth.entity.User;
import com.medirandevu.auth.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Issues and verifies the stateless access tokens used by this service.
 *
 * <p>Tokens are compact JWS strings (header.payload.signature) signed with a symmetric
 * HMAC-SHA key. Because the same key both signs and verifies, any service that needs to
 * validate these tokens must share the {@code jwt.secret} value.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(JwtProperties props) {
        // The secret is configured as Base64 so it can hold arbitrary random bytes.
        byte[] bytes = Decoders.BASE64.decode(props.secret());

        // HMAC-SHA256 (HS256) requires a key of at least 256 bits; fail fast on startup
        // instead of issuing weak tokens.
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT secret should be minimum 256 bit (32 byte)");
        }

        // JJWT picks the strongest HS algorithm the key length allows
        // (32 bytes -> HS256, 48 -> HS384, 64+ -> HS512).
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = props.expirationMs();
    }

    /**
     * Builds a signed token for the given user.
     *
     * <p>Payload layout:
     * <ul>
     *   <li>{@code sub}  – user id (read back by {@code JwtAuthFilter} as the principal name)</li>
     *   <li>{@code email} – user's email, for convenience of downstream services</li>
     *   <li>{@code role} – PATIENT / DOCTOR / ADMIN, mapped to a {@code ROLE_*} authority</li>
     *   <li>{@code iat} / {@code exp} – issue time and expiry ({@code jwt.expiration-ms} later)</li>
     * </ul>
     * The token is not encrypted, only signed: anyone can read the payload, so never put
     * secrets in it.
     */
    public String generateToken(User user){
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                // Enum is serialized to its name ("PATIENT", ...) by the JSON serializer.
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(new Date(now.getTime()+ expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * Verifies the signature and expiry of a token and returns its claims.
     *
     * @throws io.jsonwebtoken.JwtException if the token is malformed, tampered with,
     *         signed with another key, or expired
     * @throws IllegalArgumentException if the token is null or empty
     */
    public Claims parse(String token){
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}