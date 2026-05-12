package com.circleguard.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrTokenServiceUnitTest {

    private static final String SECRET = "my-qr-secret-key-for-dev-1234567890";

    @Test
    void shouldGenerateQrTokenWithAnonymousSubject() {
        QrTokenService service = new QrTokenService(SECRET, 60_000L);
        UUID anonymousId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        String token = service.generateQrToken(anonymousId);

        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals(anonymousId.toString(), claims.getSubject());
        assertTrue(claims.getExpiration().after(new Date()));
    }
}