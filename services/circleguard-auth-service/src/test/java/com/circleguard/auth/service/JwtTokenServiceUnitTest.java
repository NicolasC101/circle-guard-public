package com.circleguard.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Key;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtTokenServiceUnitTest {

    private static final String SECRET = "my-super-secret-test-key-32-chars-long";

    @Test
    void shouldGenerateTokenWithAnonymousSubjectAndPermissions() {
        JwtTokenService service = new JwtTokenService(SECRET, 3_600_000L);
        UUID anonymousId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        var authentication = new UsernamePasswordAuthenticationToken(
                "user",
                "password",
                List.of(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("READ_PRIVILEGES")
                )
        );

        String token = service.generateToken(anonymousId, authentication);

        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals(anonymousId.toString(), claims.getSubject());
        assertEquals(List.of("ROLE_ADMIN", "READ_PRIVILEGES"), claims.get("permissions", List.class));
    }
}