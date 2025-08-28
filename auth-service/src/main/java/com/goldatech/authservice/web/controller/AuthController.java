package com.goldatech.authservice.web.controller;

import com.goldatech.authservice.domain.service.AuthService;
import com.goldatech.authservice.web.dto.request.LoginRequest;
import com.goldatech.authservice.web.dto.request.RegisterRequest;
import com.goldatech.authservice.web.dto.response.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Handles user registration.
     *
     * @param request a RegisterRequest containing user details.
     * @return a ResponseEntity with an AuthResponse containing a JWT token.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * Handles user login.
     *
     * @param request a LoginRequest containing user credentials.
     * @return a ResponseEntity with an AuthResponse containing a JWT token.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
