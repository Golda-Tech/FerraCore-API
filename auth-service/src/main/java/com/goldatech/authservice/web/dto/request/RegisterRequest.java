package com.goldatech.authservice.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object (DTO) for registration requests.
 * This record is used to receive new user details from the client for registration.
 *
 * @param firstname the new user's first name
 * @param lastname the new user's last name
 * @param email the new user's email
 * @param organizationName the new user's password
 */
public record RegisterRequest(
        @NotBlank  String firstname,
        @NotBlank String lastname,
        @Email String email,
        @NotBlank String organizationName
) {}