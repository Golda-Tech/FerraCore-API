package com.goldatech.paymentservice.web.dto.response.momo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BasicUserInfoResponse(
        @JsonProperty("sub") String id,
        @JsonProperty("name") String fullName,
        @JsonProperty("given_name") String firstName,
        @JsonProperty("family_name") String lastName,
        @JsonProperty("locale") String locale,
        @JsonProperty("gender") String gender,
        @JsonProperty("birthdate") String birthdate,
        @JsonProperty("updated_at") Long updatedAt
) {}
