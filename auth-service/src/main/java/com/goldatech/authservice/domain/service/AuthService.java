package com.goldatech.authservice.domain.service;

import com.goldatech.authservice.domain.exception.UserAlreadyExistsException;
import com.goldatech.authservice.domain.model.*;
import com.goldatech.authservice.domain.repository.OtpRepository;
import com.goldatech.authservice.domain.repository.SubscriptionRepository;
import com.goldatech.authservice.domain.repository.UserRepository;
import com.goldatech.authservice.security.JwtService;
import com.goldatech.authservice.web.dto.request.LoginRequest;
import com.goldatech.authservice.web.dto.request.RegisterRequest;
import com.goldatech.authservice.web.dto.request.ResetPasswordRequest;
import com.goldatech.authservice.web.dto.request.SubscriptionCreateRequest;
import com.goldatech.authservice.web.dto.response.AuthResponse;
import com.goldatech.authservice.web.dto.response.RegistrationResponse;
import com.goldatech.authservice.web.dto.response.ResetPasswordResponse;
import com.goldatech.authservice.web.dto.response.SubscriptionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Service class containing the business logic for authentication.
 * Handles user registration and login.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SubscriptionService subscriptionService;
    private final AuthenticationManager authenticationManager;
    private final OtpRepository otpRepository;
    private final RabbitTemplate rabbitTemplate;


    @Value("${notification.exchange}")
    private String notificationExchange;

    @Value("${notification.payment.routing-key}")
    private String paymentRoutingKey;

    @Value("${notification.otp.routing-key}")
    private String otpRoutingKey;

    @Value("auth-events")
    private String authRoutingKey;

    /**
     * Registers a new user.
     *
     * @param request a RegisterRequest with user details.
     * @return an AuthResponse containing the JWT token and user details.
     * @throws UserAlreadyExistsException if a user with the given email already exists.
     */
    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("User with email " + request.email() + " already exists");
        }

        if (userRepository.existsByOrganizationName(request.organizationName())) {
            throw new UserAlreadyExistsException("Business name " + request.email() + " already exists");
        }


        if (subscriptionRepository.existsByContactEmail(request.email())) {
            throw new UserAlreadyExistsException("Subscription with email " + request.email() + " already exists");
        }

        if (subscriptionRepository.existsByOrganizationName(request.organizationName())) {
            throw new UserAlreadyExistsException("Business name " + request.email() + " already exists");
        }

        var subscription = new SubscriptionCreateRequest(
                request.organizationName(),
                request.planType(),
                request.email(),
                request.mobileNumber(),
                ""
        );
        SubscriptionResponse subscriptionResponse = subscriptionService.createSubscription(subscription);

        String tempPassword = generateRandomPassword(10);

        var user = User.builder()
                .firstname(request.firstname())
                .lastname(request.lastname())
                .email(request.email())
                .password(passwordEncoder.encode(tempPassword))
                .role(Role.USER)
                .firstTimeUser(true)
                .passwordResetRequired(true)
                .build();

        userRepository.save(user);

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId().toString());
        extraClaims.put("role", user.getRole());

        var jwtToken = jwtService.generateToken(extraClaims, user);

        // Build event but don’t send yet
        AuthEvent event = new AuthEvent(
                request.organizationName(),
                user.getEmail(),
                "ORG_REGISTERED",
                tempPassword,
                subscription.planType().displayName(),
                LocalDateTime.now()
        );

        // Register callback to send after commit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                rabbitTemplate.convertAndSend(notificationExchange, authRoutingKey, event);
            }
        });

        return new RegistrationResponse(
                jwtToken,
                request.email(),
                true,
                subscriptionResponse,
                "Organization registered successfully. Temporary password sent to email with login instructions."
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
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(); // Throws an exception if the user is not found.

        // Create extra claims
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId().toString()); // or however you get user ID
        extraClaims.put("role", user.getRole());

        // Generate a new JWT token for the authenticated user.
        var jwtToken = jwtService.generateToken(extraClaims, user);
        log.info("First timer {}, password required {} logged in successfully.", user.isFirstTimeUser(), user.isPasswordResetRequired());
        return new AuthResponse(
                jwtToken,
                user.getId(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.getRole(),
                "User logged in successfully.",
                user.isPasswordResetRequired(),
                user.isFirstTimeUser()
        );
    }

    /**
     * Login with just email and get sent OTP for verification.
     * @param destination User's email address.
     * @param channel
     * @param type
     * @return true if OTP sent successfully, false otherwise.
     */

    public boolean loginWithOtp(String destination, String password, String channel, String type) {
        //First check if user exists with the email
        Optional<User> userOpt = userRepository.findByEmail(destination);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found with email: " + destination);
        }
        //Also check if password matches
        User user = userOpt.get();
        if (user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid password for email: " + destination);
        }

        String generatedOtp = otpGenerator();

        Otp otp = Otp.builder()
                .otpCode(generatedOtp)
                .type(type)
                .message("Your OTP code is " + generatedOtp)
                .channel(channel)
                .subject(type + " Verification")
                .used(false)
                .build();

        if ("SMS".equals(channel)) {
            otp.setMobileNumber(destination);
        } else if ("EMAIL".equals(channel)) {
            otp.setEmail(destination);
        }

        otpRepository.save(otp);

        OtpEvent event = new OtpEvent(
                "SMS".equals(channel) ? destination : null,
                "EMAIL".equals(channel) ? destination : null,
                generatedOtp,
                null,
                type,
                otp.getMessage(),
                channel,
                otp.getSubject()
        );

        rabbitTemplate.convertAndSend(notificationExchange, otpRoutingKey, event);

        return true;
    }



    /**
     * Verifies the OTP for login and generates a JWT token upon successful verification.
     * @param identifier User's email address.
     * @param otp The OTP code to verify.
     * @return an AuthResponse containing the JWT token and user details.
     */
    @Transactional
    public AuthResponse verifyLoginOtp(String identifier, String otp, String channel) {
        // Verify the OTP
        Optional<Otp> otpRecordOpt;

        if ("SMS".equalsIgnoreCase(channel)) {
            otpRecordOpt = otpRepository
                    .findTopByMobileNumberAndOtpCodeAndExpiresAtAfterAndUsedFalseOrderByCreatedAtDesc(
                            identifier, otp, LocalDateTime.now()
                    );
        } else if ("EMAIL".equalsIgnoreCase(channel)) {
            otpRecordOpt = otpRepository
                    .findTopByEmailAndOtpCodeAndExpiresAtAfterAndUsedFalseOrderByCreatedAtDesc(
                            identifier, otp, LocalDateTime.now()
                    );
        } else {
            throw new IllegalArgumentException("Unsupported OTP channel: " + channel);
        }

        if (otpRecordOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        Otp otpRecord = otpRecordOpt.get();

        // Mark OTP as used
        otpRecord.setUsed(true);
        otpRepository.save(otpRecord);

        // Fetch the user by email
//        var user = repository.findByEmail(identifier)
//                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + identifier));

        //allow login from any user so that new users can be created on the fly
        var user = userRepository.findByEmail(identifier)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            //Extract name from email prefix or set default names
                            .firstname(identifier.contains("@") ? identifier.substring(0, identifier.indexOf("@")) : "User")
                            .lastname(" ") // Default last name
                            .email(identifier)
                            .password(passwordEncoder.encode(otpGenerator())) // Random password
                            .role(Role.USER) // Default role
                            .passwordResetRequired(false)
                            .firstTimeUser(true)
                            .build();
                    return userRepository.save(newUser);
                });

        // Create extra claims
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId().toString());
        extraClaims.put("role", user.getRole());

        // Generate a JWT token for the user
        var jwtToken = jwtService.generateToken(extraClaims, user);

        log.info("First timer {}, password required {} .Verify Otp", user.isFirstTimeUser(), user.isPasswordResetRequired());

        return new AuthResponse(
                jwtToken,
                user.getId(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.getRole(),
                "OTP verified and user logged in successfully.",
                user.isPasswordResetRequired(),
                user.isFirstTimeUser()
        );

    }

    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.tempPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid temporary password");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetRequired(false);
        user.setFirstTimeUser(true);
        userRepository.save(user);

        return new ResetPasswordResponse("Password reset successfully.");
    }

    public ResetPasswordResponse forgotPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));


        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        return new ResetPasswordResponse("Password changed successfully.");
    }


    private String otpGenerator() {
        int otp = ThreadLocalRandom.current().nextInt(100000, 999999); // 6 digits
        return String.valueOf(otp);
    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%&*!";
        return ThreadLocalRandom.current()
                .ints(length, 0, chars.length())
                .mapToObj(i -> String.valueOf(chars.charAt(i)))
                .collect(Collectors.joining());
    }
}
