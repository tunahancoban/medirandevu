package com.medirandevu.auth.service.classes;

import com.medirandevu.auth.dto.LoginRequest;
import com.medirandevu.auth.dto.LoginResult;
import com.medirandevu.auth.dto.RegisterRequest;
import com.medirandevu.auth.dto.UserResponse;
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

    @Transactional
    public UserResponse register(RegisterRequest request){
        String email = request.email().trim().toLowerCase();

        if(userRepository.existsByEmail(email)){
            throw new EmailAlreadyExistsException(email);
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(User.Role.PATIENT)
                .build();

        return toResponse(userRepository.save(user));

    }
    @Transactional
    public LoginResult login(LoginRequest request){
        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .filter(User::isEnabled)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        return new LoginResult(jwtService.generateToken(user), toResponse(user));

    }

    public UserResponse getCurrentUser(Long userId){
        return userRepository.findById(userId).map(this::toResponse).orElseThrow(()-> new UserNotFoundException(userId));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole().name());
    }
}
