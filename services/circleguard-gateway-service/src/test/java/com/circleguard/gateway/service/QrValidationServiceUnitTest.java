package com.circleguard.gateway.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QrValidationServiceUnitTest {

    private static final String SECRET = "my-qr-secret-key-for-dev-1234567890";

    private StringRedisTemplate redisTemplate;
    private QrValidationService service;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        service = new QrValidationService(redisTemplate);
        ReflectionTestUtils.setField(service, "qrSecret", SECRET);
    }

    @Test
    void shouldAllowClearUserWhenRedisStatusIsClear() {
        UUID anonymousId = UUID.fromString("550e8400-e29b-41d4-a716-446655440030");
        when(redisTemplate.opsForValue()).thenReturn(mock(ValueOperations.class));
        when(redisTemplate.opsForValue().get(anyString())).thenReturn("CLEAR");

        String token = buildQrToken(anonymousId);

        QrValidationService.ValidationResult result = service.validateToken(token);

        assertTrue(result.valid());
        assertTrue("GREEN".equals(result.status()));
    }

    @Test
    void shouldDenyContagiedUserWhenRedisStatusIsContagied() {
        UUID anonymousId = UUID.fromString("550e8400-e29b-41d4-a716-446655440031");
        when(redisTemplate.opsForValue()).thenReturn(mock(ValueOperations.class));
        when(redisTemplate.opsForValue().get(anyString())).thenReturn("CONTAGIED");

        String token = buildQrToken(anonymousId);

        QrValidationService.ValidationResult result = service.validateToken(token);

        assertFalse(result.valid());
        assertTrue("RED".equals(result.status()));
    }

    @Test
    void shouldRejectInvalidTokenSignature() {
        when(redisTemplate.opsForValue()).thenReturn(mock(ValueOperations.class));

        QrValidationService.ValidationResult result = service.validateToken("broken-token");

        assertFalse(result.valid());
        assertTrue("RED".equals(result.status()));
    }

    private String buildQrToken(UUID anonymousId) {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        return Jwts.builder()
                .setSubject(anonymousId.toString())
                .setIssuedAt(new java.util.Date())
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 60_000L))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}