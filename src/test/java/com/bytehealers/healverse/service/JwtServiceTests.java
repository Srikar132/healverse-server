package com.bytehealers.healverse.service;

import com.bytehealers.healverse.model.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTests {

    private static final String SECRET_A = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String SECRET_B = Base64.getEncoder().encodeToString("another-32-byte-secret-for-tests".getBytes());

    private static JwtService serviceWith(String secret, long expirationMs) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", secret);
        ReflectionTestUtils.setField(service, "expirationMs", expirationMs);
        ReflectionTestUtils.invokeMethod(service, "initKey");
        return service;
    }

    private static User user() {
        User user = new User();
        user.setId(7L);
        user.setUsername("tester");
        return user;
    }

    @Test
    void roundTripsUsername() {
        JwtService service = serviceWith(SECRET_A, 60_000);
        String token = service.generateJwtToken(user());

        assertEquals("tester", service.extractUsername(token));
    }

    @Test
    void rejectsExpiredToken() {
        JwtService service = serviceWith(SECRET_A, -1_000);
        String token = service.generateJwtToken(user());

        assertThrows(ExpiredJwtException.class, () -> service.extractUsername(token));
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        String token = serviceWith(SECRET_A, 60_000).generateJwtToken(user());

        assertThrows(JwtException.class, () -> serviceWith(SECRET_B, 60_000).extractUsername(token));
    }

    @Test
    void refusesWeakOrMalformedSecret() {
        String shortSecret = Base64.getEncoder().encodeToString(new byte[8]);

        assertThrows(IllegalStateException.class, () -> serviceWith(shortSecret, 60_000));
        assertThrows(IllegalStateException.class, () -> serviceWith("not base64 !!!", 60_000));
    }
}
