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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StageEnvironmentSmokeTest {

    private static final String AUTH_BASE_URL = System.getenv().getOrDefault("AUTH_BASE_URL", "http://host.docker.internal:30080");
    private static final String GATEWAY_BASE_URL = System.getenv().getOrDefault("GATEWAY_BASE_URL", "http://host.docker.internal:30082");
    private static final String USERNAME = System.getenv().getOrDefault("CIRCLEGUARD_USERNAME", "super_admin");
    private static final String PASSWORD = System.getenv().getOrDefault("CIRCLEGUARD_PASSWORD", "password");

    private final HttpClient httpClient = HttpClient.newHttpClient();
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

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
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
        HttpResponse<String> qrResponse = httpClient.send(qrRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, qrResponse.statusCode());

        String qrToken = objectMapper.readTree(qrResponse.body()).get("qrToken").asText();

        HttpRequest validateRequest = HttpRequest.newBuilder()
                .uri(URI.create(GATEWAY_BASE_URL + "/api/v1/gate/validate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"token\":\"" + qrToken + "\"}"))
                .build();

        HttpResponse<String> validateResponse = httpClient.send(validateRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, validateResponse.statusCode());
        JsonNode validateBody = objectMapper.readTree(validateResponse.body());
        assertTrue(validateBody.get("valid").asBoolean());
        assertEquals("GREEN", validateBody.get("status").asText());
    }

    private JsonNode login() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AUTH_BASE_URL + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"username\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}",
                        StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode());
        return objectMapper.readTree(response.body());
    }
}