package com.circleguard.auth.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StageEnvironmentSmokeTest {

    private static final String AUTH_BASE_URL = System.getenv().getOrDefault("AUTH_BASE_URL", "http://host.docker.internal:30080");
    private static final String GATEWAY_BASE_URL = System.getenv().getOrDefault("GATEWAY_BASE_URL", "http://host.docker.internal:30082");
    private static final String USERNAME = System.getenv().getOrDefault("CIRCLEGUARD_USERNAME", "super_admin");
    private static final String PASSWORD = System.getenv().getOrDefault("CIRCLEGUARD_PASSWORD", "password");

        private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldAuthenticateAgainstStageAuthAndIdentity() throws Exception {
        JsonNode login = login();

        assertNotNull(login.get("token"));
        assertNotNull(login.get("anonymousId"));
        assertEquals("Bearer", login.get("type").asText());
    }

    @Test
    void shouldGenerateQrTokenFromStageAuth() throws Exception {
        JsonNode login = login();
        String token = login.get("token").asText();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AUTH_BASE_URL + "/api/v1/auth/qr/generate"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = sendWithRetry(request, 30, Duration.ofSeconds(2));
        assertEquals(200, response.statusCode());

        JsonNode body = objectMapper.readTree(response.body());
        assertNotNull(body.get("qrToken"));
        assertTrue(body.get("qrToken").asText().length() > 10);
    }

    @Test
    void shouldValidateQrTokenAtGatewayInStage() throws Exception {
        JsonNode login = login();
        String token = login.get("token").asText();

        HttpRequest qrRequest = HttpRequest.newBuilder()
                .uri(URI.create(AUTH_BASE_URL + "/api/v1/auth/qr/generate"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<String> qrResponse = sendWithRetry(qrRequest, 30, Duration.ofSeconds(2));
        assertEquals(200, qrResponse.statusCode());

        String qrToken = objectMapper.readTree(qrResponse.body()).get("qrToken").asText();
        System.out.println("Generated QR Token: " + qrToken);

        // Properly escape the JSON using ObjectMapper
        String jsonBody = objectMapper.writeValueAsString(Map.of("token", qrToken));
        System.out.println("Request JSON body: " + jsonBody);

        HttpRequest validateRequest = HttpRequest.newBuilder()
                .uri(URI.create(GATEWAY_BASE_URL + "/api/v1/gate/validate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        JsonNode validateBody = validateUntilValid(validateRequest, 30, Duration.ofSeconds(2));
        assertTrue(validateBody.get("valid").asBoolean(), "Gateway validate response: " + validateBody.toString());
        assertEquals("GREEN", validateBody.get("status").asText(), "Gateway validate response: " + validateBody.toString());
    }

    private JsonNode login() throws IOException, InterruptedException {
        // Properly escape the JSON using ObjectMapper
        String jsonBody = objectMapper.writeValueAsString(Map.of("username", USERNAME, "password", PASSWORD));
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AUTH_BASE_URL + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = sendWithRetry(request, 30, Duration.ofSeconds(2));
        assertEquals(200, response.statusCode());
        return objectMapper.readTree(response.body());
    }

    private HttpResponse<String> sendWithRetry(HttpRequest request, int attempts, Duration delay)
            throws IOException, InterruptedException {
        IOException lastException = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            } catch (IOException exception) {
                lastException = exception;

                if (attempt == attempts) {
                    throw exception;
                }

                Thread.sleep(delay.toMillis());
            }
        }

        throw lastException;
    }

    private JsonNode validateUntilValid(HttpRequest request, int attempts, Duration delay)
            throws IOException, InterruptedException {
        JsonNode lastBody = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                assertEquals(200, response.statusCode(), "Gateway validate HTTP status, body: " + response.body());

                lastBody = objectMapper.readTree(response.body());
                System.out.println("Attempt " + attempt + ": Gateway response: " + lastBody.toString());
                
                if (lastBody.path("valid").asBoolean(false)) {
                    return lastBody;
                }

                if (attempt < attempts) {
                    Thread.sleep(delay.toMillis());
                }
            } catch (Exception e) {
                System.out.println("Attempt " + attempt + ": Exception during validation: " + e.getMessage());
                e.printStackTrace();
                if (attempt < attempts) {
                    Thread.sleep(delay.toMillis());
                }
            }
        }

        throw new AssertionError("Gateway validate never became valid. Last response: " + lastBody);
    }
}