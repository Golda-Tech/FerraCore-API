package com.goldatech.authservice.domain.service;

import com.goldatech.authservice.domain.exception.UserAlreadyExistsException;
import com.goldatech.authservice.domain.model.Role;
import com.goldatech.authservice.domain.model.User;
import com.goldatech.authservice.domain.repository.UserRepository;
import com.goldatech.authservice.security.JwtService;
import com.goldatech.authservice.web.dto.request.LoginRequest;
import com.goldatech.authservice.web.dto.request.RegisterRequest;
import com.goldatech.authservice.web.dto.response.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class containing the business logic for authentication.
 * Handles user registration and login.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Registers a new user.
     *
     * @param request a RegisterRequest with user details.
     * @return an AuthResponse containing the JWT token and user details.
     * @throws UserAlreadyExistsException if a user with the given email already exists.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if user already exists
        if (repository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("User with email " + request.email() + " already exists");
        }

        // Build a new User object from the registration request.
        var user = User.builder()
                .firstname(request.firstname())
                .lastname(request.lastname())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER) // Assign a default role
                .build();

        try {
            repository.save(user);
        } catch (Exception e) {
            // Handle potential database constraint violations (race condition fallback)
            if (e.getMessage().contains("email") || e.getMessage().contains("unique")) {
                throw new UserAlreadyExistsException("User with email " + request.email() + " already exists");
            }
            throw e;
        }

        // Generate a JWT token for the new user.
        var jwtToken = jwtService.generateToken(user);
        return new AuthResponse(
                jwtToken,
                user.getId(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.getRole(),
                "User registered successfully."
        );
    }
    


    /**
     * Authenticates a user and generates a JWT token upon successful login.
     *
     * @param request a LoginRequest with user credentials.
     * @return an AuthResponse containing the JWT token and user details.
     */
    public AuthResponse login(LoginRequest request) {
        // Authenticate the user using the AuthenticationManager.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        // Find the user by email from the repository.
        var user = repository.findByEmail(request.email())
                .orElseThrow(); // Throws an exception if the user is not found.

        // Generate a new JWT token for the authenticated user.
        var jwtToken = jwtService.generateToken(user);
        return new AuthResponse(
                jwtToken,
                user.getId(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.getRole(),
                "User logged in successfully."
        );
    }
}
