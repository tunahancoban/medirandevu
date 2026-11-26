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

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(JwtProperties props) {
        byte[] bytes = Decoders.BASE64.decode(props.secret());
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT secret should be minimum 256 bit (32 byte)");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = props.expirationMs();
    }

    public String generateToken(User user){
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(new Date(now.getTime()+ expirationMs))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token){
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}