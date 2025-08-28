package com.goldatech.authservice.web.dto.request;

import jakarta.validation.constraints.Email;

/**
 * Data Transfer Object (DTO) for registration requests.
 * This record is used to receive new user details from the client for registration.
 *
 * @param firstname the new user's first name
 * @param lastname the new user's last name
 * @param email the new user's email
 * @param password the new user's password
 */
public record RegisterRequest(
        String firstname,
        String lastname,
        @Email String email,
        String password
) {}