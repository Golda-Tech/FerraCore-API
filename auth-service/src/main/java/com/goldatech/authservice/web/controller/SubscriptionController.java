package com.goldatech.authservice.web.controller;

import com.goldatech.authservice.domain.service.SubscriptionService;
import com.goldatech.authservice.web.dto.request.SubscriptionCreateRequest;
import com.goldatech.authservice.web.dto.request.SubscriptionLoginRequest;
import com.goldatech.authservice.web.dto.request.SubscriptionUpdateRequest;
import com.goldatech.authservice.web.dto.response.ApiCredentialsResponse;
import com.goldatech.authservice.web.dto.response.SubscriptionAuthResponse;
import com.goldatech.authservice.web.dto.response.SubscriptionResponse;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@Slf4j
@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "Subscriptions", description = "Manage subscriptions and API credentials")
public class SubscriptionController {

    private final SubscriptionService service;

    public SubscriptionController(SubscriptionService service) {
        this.service = service;
    }

    @Operation(summary = "Create subscription",
            description = "Creates a new subscription for an organization.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subscription created",
                    content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/initiate")
    public SubscriptionResponse create(@Valid @RequestBody SubscriptionCreateRequest request) {
        log.info("Received request to create subscription for org: {}", request.organizationName());
        return service.createSubscription(request);
    }

    @Operation(summary = "Get all subscriptions",
            description = "Returns a list of all subscriptions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of subscriptions",
                    content = @Content(schema = @Schema(implementation = SubscriptionResponse.class)))
    })
    @GetMapping
    public List<SubscriptionResponse> getAll() {
        log.debug("Received request to fetch all subscriptions");
        return service.getAllSubscriptions();
    }

    @Operation(summary = "Authorize subscription key",
            description = "Authorizes a subscription using the provided subscription key and Authorization header.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authorization successful",
                    content = @Content(schema = @Schema(implementation = SubscriptionAuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Basic Auth", required = true)
    @PostMapping("/tokens")
    public SubscriptionAuthResponse authorizeAccessToken(@RequestHeader("Authorization") String authorization,
                                                         @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                                 description = "Basic authentication header containing API Subscription Key and API subscription Secret. Should be sent in as B64 encoded.",
                                                                 required = true,
                                                                 content = @Content(schema = @Schema(implementation = SubscriptionLoginRequest.class))
                                                         )
                                                         @RequestBody SubscriptionLoginRequest request) {
        log.info("Received subscription auth request with key: {}", request.subscriptionKey());
        return service.authorize(request, authorization);
    }

    @Operation(summary = "Get subscription by key",
            description = "Fetches a subscription using its public key.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subscription found",
                    content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @GetMapping("/{key}")
    public SubscriptionResponse getByKey(@Parameter(description = "Subscription public key", required = true) @Valid @PathVariable String key) {
        log.debug("Received request to fetch subscription by key: {}", key);
        return service.getByKey(key)
                .orElseThrow(() -> {
                    log.error("Subscription not found with key: {}", key);
                    return new RuntimeException("Subscription not found");
                });
    }

    @Operation(summary = "Update subscription",
            description = "Updates subscription details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subscription updated",
                    content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @PutMapping("/{id}")
    public SubscriptionResponse update(@Parameter(description = "Subscription ID", required = true) @PathVariable Long id,
                                       @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                               description = "Subscription update payload",
                                               required = true,
                                               content = @Content(schema = @Schema(implementation = SubscriptionUpdateRequest.class))
                                       )
                                       @RequestBody SubscriptionUpdateRequest request) {
        log.info("Received request to update subscription with id: {}", id);
        return service.updateSubscription(id, request);
    }

    @Operation(summary = "Delete subscription",
            description = "Deletes a subscription by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subscription deleted"),
            @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @DeleteMapping("/{id}")
    public void delete(@Parameter(description = "Subscription ID", required = true) @PathVariable Long id) {
        log.warn("Received request to delete subscription with id: {}", id);
        service.deleteSubscription(id);
    }

    @Operation(summary = "Regenerate credentials",
            description = "Regenerates API credentials for a subscription. This will invalidate existing credentials.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credentials regenerated",
                    content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @PostMapping("/{id}/regenerate-credentials")
    public SubscriptionResponse regenerateCredentials(@Parameter(description = "Subscription ID", required = true) @PathVariable Long id) {
        log.warn("Received request to regenerate credentials for subscription ID: {}", id);
        return service.regenerateCredentials(id);
    }

    @Operation(summary = "Get subscription credentials",
            description = "Returns only the API credentials (key and secret) for a subscription.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credentials returned",
                    content = @Content(schema = @Schema(implementation = ApiCredentialsResponse.class))),
            @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @GetMapping("/{id}/credentials")
    public ApiCredentialsResponse getCredentials(@Parameter(description = "Subscription ID", required = true) @PathVariable Long id) {
        log.debug("Received request to fetch credentials for subscription ID: {}", id);
        return service.getCredentials(id);
    }

    @Operation(summary = "Regenerate credentials by key",
            description = "Regenerates credentials using the current subscription key. Useful when calling with known key.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credentials regenerated",
                    content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @PostMapping("/regenerate-credentials")
    public SubscriptionResponse regenerateCredentialsByKey(@Parameter(description = "Current subscription key", required = true) @RequestParam String key) {
        log.warn("Received request to regenerate credentials for subscription key: {}", key);
        return service.regenerateCredentialsByKey(key);
    }
}