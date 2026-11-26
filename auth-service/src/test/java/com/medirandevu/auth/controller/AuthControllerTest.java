package com.medirandevu.auth.controller;

import com.medirandevu.auth.config.GlobalExceptionHandler;
import com.medirandevu.auth.dto.LoginRequest;
import com.medirandevu.auth.dto.LoginResult;
import com.medirandevu.auth.dto.RegisterRequest;
import com.medirandevu.auth.dto.UserResponse;
import com.medirandevu.auth.exception.EmailAlreadyExistsException;
import com.medirandevu.auth.exception.InvalidCredentialsException;
import com.medirandevu.auth.exception.UserNotFoundException;
import com.medirandevu.auth.service.classes.CookieService;
import com.medirandevu.auth.service.interfaces.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Spring context açmadan, controller + GlobalExceptionHandler'ı standalone MockMvc ile test eder.
 * Güvenlik filtresi bu katmanda yoktur; JwtAuthFilterTest ayrıca test eder.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock AuthService authService;
    @Mock CookieService cookieService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, cookieService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // ---------- register ----------

    @Test
    void register_returns201WithUser() throws Exception {
        when(authService.register(new RegisterRequest("ali@example.com", "secret1")))
                .thenReturn(new UserResponse(1L, "ali@example.com", "PATIENT"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ali@example.com\",\"password\":\"secret1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ali@example.com"))
                .andExpect(jsonPath("$.role").value("PATIENT"));
    }

    @Test
    void register_returns400WithFieldErrorsForInvalidBody() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());

        verifyNoInteractions(authService);
    }

    @Test
    void register_returns400ForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bozuk"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed request"));
    }

    @Test
    void register_returns409WhenEmailExists() throws Exception {
        when(authService.register(any())).thenThrow(new EmailAlreadyExistsException("ali@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ali@example.com\",\"password\":\"secret1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Email already exists"))
                .andExpect(jsonPath("$.detail").value(containsString("ali@example.com")));
    }

    @Test
    void register_returns409OnDataIntegrityViolation() throws Exception {
        when(authService.register(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ali@example.com\",\"password\":\"secret1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void register_returns500WithoutLeakingDetailsOnUnexpectedError() throws Exception {
        when(authService.register(any())).thenThrow(new RuntimeException("db password=xyz"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ali@example.com\",\"password\":\"secret1\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal error"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("xyz"))));
    }

    // ---------- login ----------

    @Test
    void login_returns200WithUserAndSetsCookie() throws Exception {
        UserResponse user = new UserResponse(1L, "ali@example.com", "PATIENT");
        when(authService.login(new LoginRequest("ali@example.com", "secret1")))
                .thenReturn(new LoginResult("jwt", user));
        when(cookieService.createAccessCookie("jwt"))
                .thenReturn(ResponseCookie.from("access_token", "jwt").httpOnly(true).path("/").build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ali@example.com\",\"password\":\"secret1\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("access_token=jwt")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(jsonPath("$.email").value("ali@example.com"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void login_returns401ForInvalidCredentials() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ali@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid credentials"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void login_returns400WhenPasswordBlank() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ali@example.com\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());

        verifyNoInteractions(authService);
    }

    // ---------- logout ----------

    @Test
    void logout_returns204AndClearsCookie() throws Exception {
        when(cookieService.clearAccessCookie())
                .thenReturn(ResponseCookie.from("access_token", "").maxAge(0).path("/").build());

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    // ---------- me ----------

    @Test
    void me_returnsCurrentUserFromAuthenticationName() throws Exception {
        when(authService.getCurrentUser(5L)).thenReturn(new UserResponse(5L, "doc@example.com", "DOCTOR"));

        mockMvc.perform(get("/api/auth/me")
                        .principal(new UsernamePasswordAuthenticationToken("5", null, List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.role").value("DOCTOR"));
    }

    @Test
    void me_returns404WhenUserNoLongerExists() throws Exception {
        when(authService.getCurrentUser(5L)).thenThrow(new UserNotFoundException(5L));

        mockMvc.perform(get("/api/auth/me")
                        .principal(new UsernamePasswordAuthenticationToken("5", null, List.of())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User not found"));
    }
}
