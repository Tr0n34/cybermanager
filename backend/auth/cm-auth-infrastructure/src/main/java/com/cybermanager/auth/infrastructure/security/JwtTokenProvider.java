package com.cybermanager.auth.infrastructure.security;

import com.cybermanager.auth.application.ports.TokenIssuer;
import com.cybermanager.auth.application.ports.TokenReader;
import com.cybermanager.auth.domain.model.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider implements TokenIssuer, TokenReader {
    private final SecretKey secretKey;

    public JwtTokenProvider(@Value("${cybermanager.security.jwt-secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String issue(UserAccount userAccount) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userAccount.id().value().toString())
                .claim("email", userAccount.email().value())
                .claim("roles", userAccount.roles().stream().map(Enum::name).toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(12, ChronoUnit.HOURS)))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public UUID readUserId(String token) {
        return UUID.fromString(readClaims(token).getSubject());
    }

    public Claims readClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

