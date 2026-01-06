// java
package com.goldatech.paymentservice.domain.service;

import com.goldatech.paymentservice.domain.exception.PaymentGatewayException;
import com.goldatech.paymentservice.domain.model.MomoProperties;
import com.goldatech.paymentservice.domain.model.TelcoProvider;
import com.goldatech.paymentservice.domain.model.momo.MtnToken;
import com.goldatech.paymentservice.domain.repository.MtnTokenRepository;
import com.goldatech.paymentservice.web.dto.request.momo.PreApprovalRequest;
import com.goldatech.paymentservice.web.dto.request.momo.RequestToPayRequest;
import com.goldatech.paymentservice.web.dto.response.momo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MtnMomoService {

    private final RestTemplate restTemplate;
    private final MtnTokenRepository tokenRepository;
    private final Map<TelcoProvider, MomoProperties> momoPropertiesByProvider;

    /* -------------------- Token endpoints -------------------- */

    /**
     * Retrieve a collection access token (calls /collection/token/).
     */
    public TokenResponse getCollectionToken() {
        log.debug("Requesting collection token...");
        String url = mtnProps().getBaseUrl() + "/collection/token/";
        HttpHeaders headers = createBasicAuthHeaders(mtnProps().getCollectionSubscriptionKey(), false);

        try {

            //Log the URL and headers for debugging
            log.debug("Requesting collection token from URL: {}", url);
            log.debug("Request headers: {}", headers);
            ResponseEntity<TokenResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(headers), TokenResponse.class);

            if (response.getBody() == null) {
                throw new PaymentGatewayException("Null token response from MTN Collection API");
            }

            log.debug("Collection token retrieved (expiresIn={}s)", response.getBody().expiresIn());
            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Failed to get collection token: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            boolean isFatal = e.getStatusCode().is4xxClientError();
            throw new PaymentGatewayException("Failed to get collection token: " + e.getResponseBodyAsString(), e, isFatal);
        } catch (Exception e) {
            log.error("Unexpected error getting collection token: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Unexpected error getting collection token", e);
        }
    }


    /**
     * Retrieve a disbursement access token (calls /disbursement/token/).
     */
    public TokenResponse getDisbursementToken() {
        String url = mtnProps().getBaseUrl() + "/disbursement/token/";
        HttpHeaders headers = createBasicAuthHeaders(mtnProps().getDisbursementSubscriptionKey(), true);

        try {
            ResponseEntity<TokenResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(headers), TokenResponse.class);

            if (response.getBody() == null) {
                throw new PaymentGatewayException("Null token response from MTN Disbursement API");
            }

            log.debug("Disbursement token retrieved (expiresIn={}s)", response.getBody().expiresIn());
            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Failed to get disbursement token: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            boolean isFatal = e.getStatusCode().is4xxClientError();
            throw new PaymentGatewayException("Failed to get disbursement token: " + e.getResponseBodyAsString(), e, isFatal);
        } catch (Exception e) {
            log.error("Unexpected error getting disbursement token: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Unexpected error getting disbursement token", e);
        }
    }

    /* -------------------- Collection endpoints -------------------- */

    /**
     * Initiates a Request to Pay. Returns the X-Reference-Id used (either passed or generated).
     *
     * @param request     request payload (record)
     * @param xReferenceId optional X-Reference-Id; if null a new UUID will be generated
     * @return the X-Reference-Id used for the Request To Pay
     */
    public String requestToPay(RequestToPayRequest request, String xReferenceId) {
        String url = mtnProps().getBaseUrl() + "/collection/v1_0/requesttopay";
//        String xRef = UUID.randomUUID().toString();
        log.info("RequestToPay payload - request params={}", request);

        try {
            String token = getStoredToken("COLLECTION");
            HttpHeaders headers = createBearerHeaders(token, mtnProps().getCollectionSubscriptionKey());
            headers.set("X-Callback-Url", mtnProps().getCallBackUrl());
            headers.set("X-Reference-Id", xReferenceId);

            HttpEntity<RequestToPayRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);


            if (!response.getStatusCode().is2xxSuccessful() && response.getStatusCode() != HttpStatus.ACCEPTED) {
                throw new PaymentGatewayException("Unexpected status from requestToPay: " + response.getStatusCode());
            }

            log.info("RequestToPay initiated - X-Reference-Id={}, status={}", xReferenceId, response.getStatusCode());
            return xReferenceId;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("requestToPay failed for reference {}: status={}, body={}", xReferenceId, e.getStatusCode(), e.getResponseBodyAsString());
            boolean isFatal = e.getStatusCode().is4xxClientError();
            throw new PaymentGatewayException("requestToPay failed: " + e.getResponseBodyAsString(), e, isFatal);
        } catch (Exception e) {
            log.error("Unexpected error in requestToPay for {}: {}", xReferenceId, e.getMessage(), e);
            throw new PaymentGatewayException("Unexpected error in requestToPay", e);
        }
    }

    /**
     * Get status of a RequestToPay using the X-Reference-Id.
     */
    public RequestToPayStatusResponse getRequestToPayStatus(String referenceId) {
        String url = mtnProps().getBaseUrl() + "/collection/v1_0/requesttopay/" + referenceId;
        log.info("RequestToPay url ={}", url);

        try {
            String token = getStoredToken("COLLECTION");
            HttpHeaders headers = createBearerHeaders(token, mtnProps().getCollectionSubscriptionKey());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<RequestToPayStatusResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, RequestToPayStatusResponse.class);

            if (response.getBody() == null) {
                throw new PaymentGatewayException("Null response from GetRequestToPayStatus for " + referenceId);
            }

            log.debug("RequestToPay status for {}: {}", referenceId, response.getBody());
            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("getRequestToPayStatus failed for {}: status={}, body={}", referenceId, e.getStatusCode(), e.getResponseBodyAsString());
            boolean isFatal = e.getStatusCode().is4xxClientError();
            throw new PaymentGatewayException("getRequestToPayStatus failed: " + e.getResponseBodyAsString(), e, isFatal);
        } catch (Exception e) {
            log.error("Unexpected error in getRequestToPayStatus for {}: {}", referenceId, e.getMessage(), e);
            throw new PaymentGatewayException("Unexpected error in getRequestToPayStatus", e);
        }
    }

    /**
     * Get basic user info by MSISDN:
     * GET /collection/v1_0/accountholder/MSISDN/{msisdn}/basicuserinfo
     */
    public BasicUserInfoResponse getBasicUserInfo(String msisdn) {
        String path = "/collection/v1_0/accountholder/MSISDN/" + msisdn + "/basicuserinfo";
        String url = mtnProps().getBaseUrl() + path;

        try {
            String token = getStoredToken("COLLECTION");
            HttpHeaders headers = createBearerHeaders(token, mtnProps().getCollectionSubscriptionKey());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<BasicUserInfoResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, BasicUserInfoResponse.class);

            if (response.getBody() == null) {
                throw new PaymentGatewayException("Null basic user info for msisdn: " + msisdn);
            }

            log.debug("Basic user info for {}: {}", msisdn, response.getBody());
            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("getBasicUserInfo failed for {}: status={}, body={}", msisdn, e.getStatusCode(), e.getResponseBodyAsString());
            boolean isFatal = e.getStatusCode().is4xxClientError();
            throw new PaymentGatewayException("getBasicUserInfo failed: " + e.getResponseBodyAsString(), e, isFatal);
        } catch (Exception e) {
            log.error("Unexpected error in getBasicUserInfo for {}: {}", msisdn, e.getMessage(), e);
            throw new PaymentGatewayException("Unexpected error in getBasicUserInfo", e);
        }
    }

    /* -------------------- Pre-approval Endpoints -------------------- */

    /**
     * Preapproval mandate creation endpoint
     * POST /collection/v2_0/preapproval
     */
    public PreApprovalResponse createPreApprovalMandate(PreApprovalRequest request) {
        String url = mtnProps().getBaseUrl() + "/collection/v2_0/preapproval";
        log.info("CreatePreApprovalMandate payload - request params={}", request);
        try {
            String token = getStoredToken("COLLECTION");
            HttpHeaders headers = createBearerHeaders(token, mtnProps().getCollectionSubscriptionKey());
            headers.set("X-Callback-Url", mtnProps().getCallBackUrl());
            String xRef = UUID.randomUUID().toString();
            headers.set("X-Reference-Id", xRef);

            HttpEntity<PreApprovalRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<PreApprovalResponse> response =
                    restTemplate.exchange(url, HttpMethod.POST, entity, PreApprovalResponse.class);

            log.info("CreatePreApprovalMandate response status - {}", response.getStatusCode());

            // Accept any 2xx success status (covers 202, 201, 200, etc.)
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new PaymentGatewayException("Unexpected message from createPreApprovalMandate: status=" + response.getStatusCode());
            }

            PreApprovalResponse body = response.getBody();
            if (body != null) {
                log.info("PreApprovalMandate created - X-Reference-Id={}, status={}", xRef, response.getStatusCode());
                return body;
            }

            log.info("PreApprovalMandate created (no body) - X-Reference-Id={}, status={}", xRef, response.getStatusCode());
            return new PreApprovalResponse(xRef, "Pre-approval mandate creation is pending approval.");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("createPreApprovalMandate failed: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            boolean isFatal = e.getStatusCode().is4xxClientError();
            throw new PaymentGatewayException("createPreApprovalMandate failed: " + e.getResponseBodyAsString(), e, isFatal);
        } catch (Exception e) {
            log.error("Unexpected error in createPreApprovalMandate: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Unexpected error in createPreApprovalMandate", e);
        }
    }

    /**
     * Preapproval mandate status endpoint
     * GET /collection/v2_0/preapproval/{referenceId}
     */
    public PreApprovalStatusResponse getPreApprovalStatus(String referenceId) {
        String url = mtnProps().getBaseUrl() + "/collection/v2_0/preapproval/" + referenceId;
        log.info("GetPreApprovalStatus url={}", url);

        try {
            String token = getStoredToken("COLLECTION");
            HttpHeaders headers = createBearerHeaders(token, mtnProps().getCollectionSubscriptionKey());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<PreApprovalStatusResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, PreApprovalStatusResponse.class);

            if (response.getBody() == null) {
                throw new PaymentGatewayException("Null response from GetPreApprovalStatus for " + referenceId);
            }

            log.debug("PreApprovalStatus for {}: {}", referenceId, response.getBody());
            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("getPreApprovalStatus failed for {}: status={}, body={}", referenceId, e.getStatusCode(), e.getResponseBodyAsString());
            boolean isFatal = e.getStatusCode().is4xxClientError();
            throw new PaymentGatewayException("getPreApprovalStatus failed: " + e.getResponseBodyAsString(), e, isFatal);
        } catch (Exception e) {
            log.error("Unexpected error in getPreApprovalStatus for {}: {}", referenceId, e.getMessage(), e);
            throw new PaymentGatewayException("Unexpected error in getPreApprovalStatus", e);
        }
    }

    /**
     * Fetch all approved preapproval mandates
     * GET collection/v1_0/preapprovals/{accountHolderIdType}/{accountHolderId}
     */
    public List<ApprovedPreapproval> getApprovedPreapprovals(String accountHolderIdType, String accountHolderId) {
        String url = mtnProps().getBaseUrl() + "/collection/v1_0/preapprovals/" + accountHolderIdType + "/" + accountHolderId;
        log.info("GetApprovedPreapprovals url={}", url);

        ParameterizedTypeReference<List<ApprovedPreapproval>> responseType =
                new ParameterizedTypeReference<List<ApprovedPreapproval>>() {};

        try {
            String token = getStoredToken("COLLECTION");
            HttpHeaders headers = createBearerHeaders(token, mtnProps().getCollectionSubscriptionKey());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<ApprovedPreapproval>> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, responseType);

            List<ApprovedPreapproval> approvedList = response.getBody();

            if (approvedList == null) {
                // Handle cases where the body might be null (e.g., if the status is 204 No Content)
                log.warn("Null or empty response body from GetApprovedPreapprovals for {}", accountHolderId);
                return List.of(); // Return an empty list instead of null
            }

            log.debug("ApprovedPreapprovals for {}: {}", accountHolderId, approvedList);
            return approvedList;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("getApprovedPreapprovals failed for {}: status={}, body={}", accountHolderId, e.getStatusCode(), e.getResponseBodyAsString());
            boolean isFatal = e.getStatusCode().is4xxClientError();
            throw new PaymentGatewayException("getApprovedPreapprovals failed: " + e.getResponseBodyAsString(), e, isFatal);
        } catch (Exception e) {
            log.error("Unexpected error in getApprovedPreapprovals for {}: {}", accountHolderId, e.getMessage(), e);
            throw new PaymentGatewayException("Unexpected error in getApprovedPreapprovals", e);
        }
    }

    /**
     * Cancel a preapproval mandate
     * collection/v1_0/preapproval/{preapprovalid}
     * DELETE
     */
    public boolean cancelPreapprovalMandate(String preapprovalId) {
        String url = mtnProps().getBaseUrl() + "/collection/v1_0/preapproval/" + preapprovalId;
        log.info("CancelPreapprovalMandate url={}", url);

        try {
            String token = getStoredToken("COLLECTION");
            HttpHeaders headers = createBearerHeadersForPreApprovalStatus(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Void> response =
                    restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new PaymentGatewayException("Unexpected status from cancelPreapprovalMandate: " + response.getStatusCode());
            }

            log.info("PreapprovalMandate {} cancelled successfully, status={}", preapprovalId, response.getStatusCode());
            return true;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("cancelPreapprovalMandate failed for {}: status={}, body={}", preapprovalId, e.getStatusCode(), e.getResponseBodyAsString());
            boolean isFatal = e.getStatusCode().is4xxClientError();
            throw new PaymentGatewayException("cancelPreapprovalMandate failed: " + e.getResponseBodyAsString(), e, isFatal);
        } catch (Exception e) {
            log.error("Unexpected error in cancelPreapprovalMandate for {}: {}", preapprovalId, e.getMessage(), e);
            throw new PaymentGatewayException("Unexpected error in cancelPreapprovalMandate", e);
        }
    }





    /* -------------------- Helpers -------------------- */

    private HttpHeaders createBasicAuthHeaders(String subscriptionKey, boolean includeEnv) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Ocp-Apim-Subscription-Key", subscriptionKey);

        String auth = subscriptionKey + ":" + UUID.randomUUID().toString().toLowerCase();
        String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + mtnProps().getBasicAuthToken());

        if (includeEnv && mtnProps().getEnvironment() != null && !mtnProps().getEnvironment().isBlank()) {
            headers.set("X-Target-Environment", mtnProps().getEnvironment());
        }

        return headers;
    }

    private HttpHeaders createBearerHeaders(String token, String subscriptionKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Ocp-Apim-Subscription-Key", subscriptionKey);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (mtnProps().getEnvironment() != null && !mtnProps().getEnvironment().isBlank()) {
            headers.set("X-Target-Environment", mtnProps().getEnvironment());
        }
        return headers;
    }

    private HttpHeaders createBearerHeadersForPreApprovalStatus(String token) {
        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (mtnProps().getEnvironment() != null && !mtnProps().getEnvironment().isBlank()) {
            headers.set("X-Target-Environment", mtnProps().getEnvironment());
        }
        return headers;
    }

    private String getStoredToken(String type) {
        return tokenRepository.findTopByTypeOrderByCreatedAtDesc(type)
                .filter(token -> token.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(MtnToken::getAccessToken)
                .orElseThrow(() -> new PaymentGatewayException("No valid " + type + " token found. Run scheduler first."));
    }

    private MomoProperties mtnProps() {
        MomoProperties props = momoPropertiesByProvider.get(TelcoProvider.MTN);
        if (props == null) {
            throw new PaymentGatewayException("MTN configuration not available");
        }
        return props;
    }

}
