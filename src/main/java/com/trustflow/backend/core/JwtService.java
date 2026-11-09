package com.trustflow.backend.core;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {
    private final Algorithm algorithm;
    private final long expMinutes;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expMinutes:1440}") long expMinutes
    ) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 characters");
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.expMinutes = expMinutes;
    }

    public String issue(String subject, Long userId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expMinutes * 60);
        return JWT.create()
                .withSubject(subject)
                .withClaim("user_id", userId)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(exp))
                .sign(algorithm);
    }

    public DecodedJWT verify(String token) {
        return JWT.require(algorithm).build().verify(token);
    }
}

