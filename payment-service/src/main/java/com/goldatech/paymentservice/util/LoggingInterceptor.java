package com.goldatech.paymentservice.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.*;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Slf4j
public class LoggingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

        logRequest(request, body);

        ClientHttpResponse response = execution.execute(request, body);

        logResponse(response);

        return response;
    }

    private void logRequest(HttpRequest request, byte[] body) throws IOException {
        log.info("=== MTN OUTGOING REQUEST ===");
        log.info("URI         : {}", request.getURI());
        log.info("Method      : {}", request.getMethod());
        log.info("Headers     : {}", request.getHeaders());
        log.info("Body        : {}", new String(body, StandardCharsets.UTF_8));
    }

    private void logResponse(ClientHttpResponse response) throws IOException {
        String responseBody = new BufferedReader(
                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));

        log.info("=== MTN INCOMING RESPONSE ===");
        log.info("Status code : {}", response.getStatusCode());
        log.info("Status text : {}", response.getStatusText());
        log.info("Headers     : {}", response.getHeaders());
        log.info("Body        : {}", responseBody);
    }
}