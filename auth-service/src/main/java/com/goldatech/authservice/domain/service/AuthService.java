package com.goldatech.authservice.domain.service;

import com.goldatech.authservice.domain.exception.UserAlreadyExistsException;
import com.goldatech.authservice.domain.model.Otp;
import com.goldatech.authservice.domain.model.OtpEvent;
import com.goldatech.authservice.domain.model.Role;
import com.goldatech.authservice.domain.model.User;
import com.goldatech.authservice.domain.repository.OtpRepository;
import com.goldatech.authservice.domain.repository.UserRepository;
import com.goldatech.authservice.security.JwtService;
import com.goldatech.authservice.web.dto.request.LoginRequest;
import com.goldatech.authservice.web.dto.request.RegisterRequest;
import com.goldatech.authservice.web.dto.response.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final OtpRepository otpRepository;
    private final RabbitTemplate rabbitTemplate;


    @Value("${notification.exchange}")
    private String notificationExchange;

    @Value("${notification.payment.routing-key}")
    private String paymentRoutingKey;

    @Value("${notification.otp.routing-key}")
    private String otpRoutingKey;

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

        // Create extra claims
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId().toString()); // or however you get user ID
        extraClaims.put("role", user.getRole());

        // Generate a JWT token for the new user.
        var jwtToken = jwtService.generateToken(extraClaims, user);
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

        // Create extra claims
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId().toString()); // or however you get user ID
        extraClaims.put("role", user.getRole());

        // Generate a new JWT token for the authenticated user.
        var jwtToken = jwtService.generateToken(extraClaims, user);
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

    /**
     * Login with just email and get sent OTP for verification.
     * @param destination User's email address.
     * @param channel
     * @param type
     * @return true if OTP sent successfully, false otherwise.
     */

    public boolean loginWithOtp(String destination, String password, String channel, String type) {
        //First check if user exists with the email
        Optional<User> userOpt = repository.findByEmail(destination);
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
        var user = repository.findByEmail(identifier)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            //Extract name from email prefix or set default names
                            .firstname(identifier.contains("@") ? identifier.substring(0, identifier.indexOf("@")) : "User")
                            .lastname(" ") // Default last name
                            .email(identifier)
                            .password(passwordEncoder.encode(otpGenerator())) // Random password
                            .role(Role.USER) // Default role
                            .build();
                    return repository.save(newUser);
                });

        // Create extra claims
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId().toString());
        extraClaims.put("role", user.getRole());

        // Generate a JWT token for the user
        var jwtToken = jwtService.generateToken(extraClaims, user);
        return new AuthResponse(
                jwtToken,
                user.getId(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.getRole(),
                "OTP verified and user logged in successfully."
        );

    }

    private String otpGenerator() {
        int otp = ThreadLocalRandom.current().nextInt(100000, 999999); // 6 digits
        return String.valueOf(otp);
    }
}
