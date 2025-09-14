package com.goldatech.paymentservice.web.dto.response.momo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BasicUserInfoResponse(
        String displayName,
        String firstName,
        String middleName,
        String lastName,
        String msisdn,
        String nationality,
        @JsonProperty("dateOfBirth") String dateOfBirth
        // add more fields as required by your integration; unknown fields are ignored
) {}
