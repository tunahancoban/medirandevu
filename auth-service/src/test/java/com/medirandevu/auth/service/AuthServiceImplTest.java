package com.medirandevu.auth.service;

import com.medirandevu.auth.dto.LoginRequest;
import com.medirandevu.auth.dto.LoginResult;
import com.medirandevu.auth.dto.RegisterRequest;
import com.medirandevu.auth.dto.UserResponse;
import com.medirandevu.auth.entity.User;
import com.medirandevu.auth.exception.EmailAlreadyExistsException;
import com.medirandevu.auth.exception.InvalidCredentialsException;
import com.medirandevu.auth.exception.UserNotFoundException;
import com.medirandevu.auth.repository.UserRepository;
import com.medirandevu.auth.service.classes.AuthServiceImpl;
import com.medirandevu.auth.service.classes.JwtService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    @InjectMocks AuthServiceImpl authService;

    private static User user(boolean enabled) {
        return User.builder()
                .id(1L)
                .email("ali@example.com")
                .passwordHash("hashed")
                .role(User.Role.PATIENT)
                .enabled(enabled)
                .build();
    }

    @Nested
    class Register {

        @Test
        void savesNormalizedEmailHashedPasswordAndPatientRole() {
            when(userRepository.existsByEmail("ali@example.com")).thenReturn(false);
            when(passwordEncoder.encode("secret1")).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });

            UserResponse response = authService.register(new RegisterRequest("  Ali@Example.COM ", "secret1"));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();
            assertThat(saved.getEmail()).isEqualTo("ali@example.com");
            assertThat(saved.getPasswordHash()).isEqualTo("hashed");
            assertThat(saved.getRole()).isEqualTo(User.Role.PATIENT);
            assertThat(saved.isEnabled()).isTrue();

            assertThat(response).isEqualTo(new UserResponse(1L, "ali@example.com", "PATIENT"));
        }

        @Test
        void throwsWhenEmailAlreadyExists() {
            when(userRepository.existsByEmail("ali@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(new RegisterRequest("ALI@example.com", "secret1")))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining("ali@example.com");

            verify(userRepository, never()).save(any());
            verifyNoInteractions(passwordEncoder);
        }
    }

    @Nested
    class Login {

        @Test
        void returnsTokenAndUserForValidCredentials() {
            User user = user(true);
            when(userRepository.findByEmail("ali@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("secret1", "hashed")).thenReturn(true);
            when(jwtService.generateToken(user)).thenReturn("jwt");

            LoginResult result = authService.login(new LoginRequest(" ALI@example.com ", "secret1"));

            assertThat(result.token()).isEqualTo("jwt");
            assertThat(result.user()).isEqualTo(new UserResponse(1L, "ali@example.com", "PATIENT"));
        }

        @Test
        void throwsWhenUserNotFound() {
            when(userRepository.findByEmail("ali@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(new LoginRequest("ali@example.com", "secret1")))
                    .isInstanceOf(InvalidCredentialsException.class);

            verifyNoInteractions(jwtService);
        }

        @Test
        void throwsWhenPasswordDoesNotMatch() {
            when(userRepository.findByEmail("ali@example.com")).thenReturn(Optional.of(user(true)));
            when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(new LoginRequest("ali@example.com", "wrong")))
                    .isInstanceOf(InvalidCredentialsException.class);

            verifyNoInteractions(jwtService);
        }

        @Test
        void throwsWhenUserDisabledWithoutCheckingPassword() {
            when(userRepository.findByEmail("ali@example.com")).thenReturn(Optional.of(user(false)));

            assertThatThrownBy(() -> authService.login(new LoginRequest("ali@example.com", "secret1")))
                    .isInstanceOf(InvalidCredentialsException.class);

            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verifyNoInteractions(jwtService);
        }
    }

    @Nested
    class GetCurrentUser {

        @Test
        void returnsUserResponse() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user(true)));

            assertThat(authService.getCurrentUser(1L))
                    .isEqualTo(new UserResponse(1L, "ali@example.com", "PATIENT"));
        }

        @Test
        void throwsWhenUserMissing() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getCurrentUser(99L))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }
}
