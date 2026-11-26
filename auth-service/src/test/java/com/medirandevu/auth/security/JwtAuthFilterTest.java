package com.medirandevu.auth.security;

import com.medirandevu.auth.service.classes.CookieService;
import com.medirandevu.auth.service.classes.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock JwtService jwtService;
    @Mock CookieService cookieService;
    @Mock FilterChain chain;

    @InjectMocks JwtAuthFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static Claims claims(String subject, String role) {
        return Jwts.claims().subject(subject).add("role", role).build();
    }

    @Test
    void noToken_leavesContextEmptyAndContinuesChain() throws Exception {
        when(cookieService.extractToken(request)).thenReturn(null);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void validCookieToken_authenticatesWithUserIdAndRole() throws Exception {
        when(cookieService.extractToken(request)).thenReturn("cookie-jwt");
        when(jwtService.parse("cookie-jwt")).thenReturn(claims("7", "DOCTOR"));

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("7");
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_DOCTOR");
        verify(chain).doFilter(request, response);
    }

    @Test
    void bearerHeader_usedWhenNoCookie() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer header-jwt");
        when(cookieService.extractToken(request)).thenReturn(null);
        when(jwtService.parse("header-jwt")).thenReturn(claims("3", "PATIENT"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("3");
    }

    @Test
    void cookieTakesPrecedenceOverBearerHeader() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer header-jwt");
        when(cookieService.extractToken(request)).thenReturn("cookie-jwt");
        when(jwtService.parse("cookie-jwt")).thenReturn(claims("1", "ADMIN"));

        filter.doFilter(request, response, chain);

        verify(jwtService).parse("cookie-jwt");
        verify(jwtService, never()).parse("header-jwt");
    }

    @Test
    void nonBearerAuthorizationHeader_isIgnored() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz");
        when(cookieService.extractToken(request)).thenReturn(null);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService);
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidToken_clearsContextAndStillContinuesChain() throws Exception {
        when(cookieService.extractToken(any(HttpServletRequest.class))).thenReturn("bad");
        when(jwtService.parse("bad")).thenThrow(new MalformedJwtException("bad token"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(200); // 401 kararı entry point'e bırakılır
        verify(chain).doFilter(request, response);
    }

    @Test
    void illegalArgumentFromParser_isHandledLikeInvalidToken() throws Exception {
        when(cookieService.extractToken(request)).thenReturn("");
        when(jwtService.parse("")).thenThrow(new IllegalArgumentException("empty"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }
}
