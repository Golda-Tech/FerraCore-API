package com.goldatech.authservice.web.dto.request;

import jakarta.validation.constraints.Email;

/**
 * Data Transfer Object (DTO) for login requests.
 * This record is used to receive user credentials from the client for login.
 *
 * @param email the user's email
 * @param password the user's password
 */
public record LoginRequest(
        @Email String email,
        String password
) {}
