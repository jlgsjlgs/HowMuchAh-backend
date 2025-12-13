package com.jlgs.howmuchah.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@TestConfiguration
public class TestSecurityConfig {

    public static final String TEST_TOKEN = "test-token";
    public static final String TEST_USER_ID = "123e4567-e89b-12d3-a456-426614174000";
    public static final String TEST_EMAIL = "test@example.com";
    public static final String TEST_NAME = "Test User";

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        // Return a custom JwtDecoder that creates a mock JWT
        return token -> createMockJwt();
    }

    private Jwt createMockJwt() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "HS256");
        headers.put("typ", "JWT");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", TEST_USER_ID);
        claims.put("email", TEST_EMAIL);
        claims.put("name", TEST_NAME);
        claims.put("iat", Instant.now().getEpochSecond());
        claims.put("exp", Instant.now().plusSeconds(3600).getEpochSecond());

        return new Jwt(
                TEST_TOKEN,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                headers,
                claims
        );
    }
}