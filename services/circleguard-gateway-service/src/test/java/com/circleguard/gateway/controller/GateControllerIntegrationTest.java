package com.circleguard.gateway.controller;

import com.circleguard.gateway.service.QrValidationService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Key;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GateController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(QrValidationService.class)
@TestPropertySource(properties = {
        "qr.secret=my-qr-secret-key-for-dev-1234567890"
})
class GateControllerIntegrationTest {

    private static final String QR_SECRET = "my-qr-secret-key-for-dev-1234567890";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUpRedisMock() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        assertNotNull(valueOperations);
    }

    @Test
    void shouldReturnGreenResultWhenQrTokenIsValidAndRedisIsClear() throws Exception {
        UUID anonymousId = UUID.fromString("550e8400-e29b-41d4-a716-446655440100");
        when(valueOperations.get("user:status:" + anonymousId)).thenReturn("CLEAR");

        mockMvc.perform(post("/api/v1/gate/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + buildQrToken(anonymousId) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.status").value("GREEN"))
                .andExpect(jsonPath("$.message").value("Welcome to Campus"));
    }

    @Test
    void shouldReturnRedResultWhenUserIsContagiedEvenWithValidQrToken() throws Exception {
        UUID anonymousId = UUID.fromString("550e8400-e29b-41d4-a716-446655440101");
        when(valueOperations.get("user:status:" + anonymousId)).thenReturn("CONTAGIED");

        mockMvc.perform(post("/api/v1/gate/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + buildQrToken(anonymousId) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.status").value("RED"))
                .andExpect(jsonPath("$.message").value("Access Denied: Health Risk Detected"));
    }

    private String buildQrToken(UUID anonymousId) {
        Key key = Keys.hmacShaKeyFor(QR_SECRET.getBytes());
        return Jwts.builder()
                .setSubject(anonymousId.toString())
                .setIssuedAt(new java.util.Date())
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 60_000L))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}