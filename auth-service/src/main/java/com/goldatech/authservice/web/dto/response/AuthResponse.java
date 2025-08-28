package com.goldatech.authservice.web.dto.response;

import com.goldatech.authservice.domain.model.Role;

/**
 * Data Transfer Object (DTO) for authentication responses.
 * This record is used to return the JWT token to the client after a successful login or registration,
 * along with basic user details and a message.
 *
 * @param token the JWT token
 * @param id the user's unique ID
 * @param firstname the user's first name
 * @param lastname the user's last name
 * @param email the user's email
 * @param role the user's role
 * @param message a descriptive message about the operation's success
 */
public record AuthResponse(
        String token,
        Integer id,
        String firstname,
        String lastname,
        String email,
        Role role,
        String message
) {}