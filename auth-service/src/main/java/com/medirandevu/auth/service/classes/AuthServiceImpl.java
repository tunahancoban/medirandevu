package com.medirandevu.auth.service.classes;

import com.medirandevu.auth.dto.*;
import com.medirandevu.auth.entity.User;
import com.medirandevu.auth.exception.EmailAlreadyExistsException;
import com.medirandevu.auth.exception.InvalidCredentialsException;
import com.medirandevu.auth.exception.UserNotFoundException;
import com.medirandevu.auth.repository.UserRepository;
import com.medirandevu.auth.service.interfaces.AuthService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /** Public self-registration always creates a PATIENT, so callers cannot choose their role. */
    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        return createUser(request.email(), request.password(), User.Role.PATIENT);
    }

    /** Doctor accounts are created by an admin only (enforced in AuthController via @PreAuthorize). */
    @Override
    @Transactional
    public UserResponse createDoctorAccount(CreateDoctorAccountRequest request) {
        return createUser(request.email(), request.password(), User.Role.DOCTOR);
    }

    private UserResponse createUser(String rawEmail, String rawPassword, User.Role role) {
        // Normalize so "Ali@Example.com " and "ali@example.com" are the same account.
        String email = rawEmail.trim().toLowerCase();

        // Fast, friendly check. Two concurrent requests can still both pass it; the UNIQUE
        // constraint on users.email then rejects the second one, and GlobalExceptionHandler
        // maps that DataIntegrityViolationException to 409 as well.
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        // Only the BCrypt hash is stored, never the plain password.
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .build();
        return toResponse(userRepository.save(user));
    }

    /**
     * Checks the credentials and issues a JWT. The controller puts the token into the
     * cookie; only the user data goes into the response body.
     */
    @Transactional
    public LoginResult login(LoginRequest request){
        String email = request.email().trim().toLowerCase();

        // Unknown email, disabled account and wrong password all end in the same
        // InvalidCredentialsException, so the response doesn't reveal which emails are registered.
        // The enabled check runs first, so disabled accounts skip the costly BCrypt comparison.
        User user = userRepository.findByEmail(email)
                .filter(User::isEnabled)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        return new LoginResult(jwtService.generateToken(user), toResponse(user));

    }

    public UserResponse getCurrentUser(Long userId){
        return userRepository.findById(userId).map(this::toResponse).orElseThrow(()-> new UserNotFoundException(userId));
    }

    // Maps the entity to a DTO so the password hash never leaves the service layer.
    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole().name());
    }
}
