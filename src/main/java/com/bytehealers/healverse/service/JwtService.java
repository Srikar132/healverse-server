package com.bytehealers.healverse.service;

import com.bytehealers.healverse.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtService {

    // HS256 needs a key of at least 256 bits
    private static final int MIN_SECRET_BYTES = 32;

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms:604800000}")
    private long expirationMs;

    private SecretKey key;

    @PostConstruct
    void initKey() {
        byte[] secretBytes;
        try {
            secretBytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException e) {
            throw new IllegalStateException("app.jwt.secret must be a base64-encoded value", e);
        }
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret must decode to at least " + MIN_SECRET_BYTES + " bytes (generate with: openssl rand -base64 32)");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    public String generateJwtToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    // Throws JwtException (signature, malformed, expired) when the token is not acceptable
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Returns the token's subject. Parsing verifies the signature and expiry, so a returned value
     * means the token is valid; anything else throws a JwtException.
     */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }
}
