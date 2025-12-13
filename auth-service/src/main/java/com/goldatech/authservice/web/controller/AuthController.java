package com.goldatech.authservice.web.controller;

import com.goldatech.authservice.domain.service.AuthService;
import com.goldatech.authservice.domain.service.ProfileService;
import com.goldatech.authservice.web.dto.request.*;
import com.goldatech.authservice.web.dto.response.AuthResponse;
import com.goldatech.authservice.web.dto.response.RegistrationResponse;
import com.goldatech.authservice.web.dto.response.ResetPasswordResponse;
import com.goldatech.authservice.web.dto.response.UserProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ProfileService profileService;

    /**
     * Handles user registration.
     *
     * @param request a RegisterRequest containing user details.
     * @return a ResponseEntity with an AuthResponse containing a JWT token.
     */
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * Handles user login.
     *
     * @param request a LoginRequest containing user credentials.
     * @return a ResponseEntity with an AuthResponse containing a JWT token.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }




    /**
     * Health check endpoint to verify the service is running.
     *
     * @return a ResponseEntity with a simple status message.
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Auth service is up and running.");
    }


    /** * Endpoint to verify a user's OTP.
     *
     * @param identifier the email of the user to verify.
     * @param channel   the channel through which the OTP was sent (e.g., EMAIL, SMS).
     * @param otp the OTP code to verify.
     * @return a ResponseEntity with a success message if verification is successful.
     */
    @GetMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestParam String identifier, @RequestParam String channel, @RequestParam String otp) {
        AuthResponse authResponse  = authService.verifyLoginOtp(identifier, otp, channel);
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Endpoint to send a login OTP to the user via the specified channel.
     * @param destination the email or phone number to send the OTP to.
     * @param channel the channel to send the OTP through (e.g., EMAIL, SMS
     * @param type the type of OTP (e.g., LOGIN, RESET_PASSWORD)
     * @return a ResponseEntity with a success message if the OTP was sent successfully.
     */
    @GetMapping("/send-login-otp")
    public ResponseEntity<String> sendLoginOtp(@RequestParam String destination,@RequestParam String password, @RequestParam String channel, @RequestParam String type) {
        authService.loginWithOtp(destination, password, channel, type);
        return ResponseEntity.ok("Login OTP sent successfully.");
    }


    @PostMapping("/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        ResetPasswordResponse response =  authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ResetPasswordResponse> forgotPassword(@Valid @RequestBody ResetPasswordRequest request) {
        ResetPasswordResponse response =  authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    // ============= PROFILE ENDPOINTS =============

    /**
     * Gets the complete profile for the authenticated user including organization and subscription details.
     *
     * @param authentication the authenticated user's security context
     * @return UserProfileResponse with complete profile data
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        log.info("Fetching profile for authenticated user");
        String email = authentication.getName(); // Gets the email from JWT
        UserProfileResponse profile = profileService.getUserProfile(email);
        return ResponseEntity.ok(profile);
    }

    /**
     * Updates the authenticated user's profile information.
     *
     * @param authentication the authenticated user's security context
     * @param request the profile update request
     * @return updated UserProfileResponse
     */
    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileUpdateRequest request) {
        log.info("Updating profile for authenticated user");
        String email = authentication.getName();
        UserProfileResponse profile = profileService.updateProfile(email, request);
        return ResponseEntity.ok(profile);
    }

    /**
     * Updates the organization details for the authenticated user.
     *
     * @param authentication the authenticated user's security context
     * @param request the organization update request
     * @return updated UserProfileResponse
     */
    @PutMapping("/profile/organization")
    public ResponseEntity<UserProfileResponse> updateOrganization(
            Authentication authentication,
            @Valid @RequestBody OrganizationUpdateRequest request) {
        log.info("Updating organization for authenticated user");
        String email = authentication.getName();
        UserProfileResponse profile = profileService.updateOrganization(email, request);
        return ResponseEntity.ok(profile);
    }


    /**
     * Updates the subscription details for the authenticated user.
     *
     * @param authentication the authenticated user's security context
     * @param callbackUrl the new callback URL
     * @return updated UserProfileResponse
     */
    @PutMapping("/profile/callback")
    public ResponseEntity<UserProfileResponse> updateOrganization(
            Authentication authentication,
            @Valid @RequestBody String callbackUrl) {
        log.info("Updating callbackUrl for authenticated user");
        String email = authentication.getName();
        UserProfileResponse profile = profileService.updateCallbackUrl(email, callbackUrl);
        return ResponseEntity.ok(profile);
    }

    /**
     * Regenerates API credentials (subscription key and secret) for the authenticated user.
     * WARNING: This will invalidate the current credentials.
     *
     * @param authentication the authenticated user's security context
     * @return updated UserProfileResponse with new credentials
     */
    @PostMapping("/profile/regenerate-credentials")
    public ResponseEntity<UserProfileResponse> regenerateApiCredentials(Authentication authentication) {
        log.warn("Regenerating API credentials for authenticated user");
        String email = authentication.getName();
        UserProfileResponse profile = profileService.regenerateApiCredentials(email);
        return ResponseEntity.ok(profile);
    }

}
