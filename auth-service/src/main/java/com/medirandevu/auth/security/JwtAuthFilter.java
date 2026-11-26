package com.medirandevu.auth.security;

import com.medirandevu.auth.service.classes.CookieService;
import com.medirandevu.auth.service.classes.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import java.util.List;

/**
 * Authenticates each request from its JWT, since the service keeps no HTTP session.
 *
 * <p>The filter never rejects a request itself. If the token is missing or invalid the
 * request simply continues unauthenticated, and Spring Security's authorization rules
 * (see {@code SecurityConfig}) decide whether to return 401 or let it through to a public
 * endpoint.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final CookieService cookieService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException{
        String token = resolveToken(request);

        if (token != null) {
            try {
                // Verifies signature + expiry; throws if anything is wrong.
                Claims claims = jwtService.parse(token);
                String role = claims.get("role", String.class);

                // Principal = user id from "sub", so controllers get it via authentication.getName().
                // Credentials are null because the token has already been verified.
                // The "ROLE_" prefix is what hasRole('ADMIN') / @PreAuthorize expect.
                // Passing authorities to this constructor marks the token as authenticated.
                var authentication = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                // Invalid/expired token: treat the caller as anonymous rather than failing here.
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Finds the token on the request. The cookie (browser clients) is checked first;
     * the {@code Authorization: Bearer <token>} header is a fallback for non-browser
     * clients such as other services, Postman or Swagger.
     */
    private String resolveToken(HttpServletRequest request) {
        String token = cookieService.extractToken(request);
        if (token != null) return token;

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7); // strip "Bearer "
        }
        return null;
    }

}
