package com.bytehealers.healverse.service;

import com.bytehealers.healverse.exception.TooManyRequestsException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory brute-force throttle for /auth/login. Failures are counted per client address + username
 * (not per username alone, so an attacker cannot lock a victim out), and the counter expires on its own.
 * Single-node only; move to a shared store if the API is ever scaled out.
 */
@Service
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;

    private final Cache<String, AtomicInteger> failures = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .maximumSize(10_000)
            .build();

    public void checkAllowed(String clientAddress, String username) {
        AtomicInteger count = failures.getIfPresent(key(clientAddress, username));
        if (count != null && count.get() >= MAX_FAILURES) {
            throw new TooManyRequestsException("Too many failed login attempts. Try again in a few minutes.");
        }
    }

    public void recordFailure(String clientAddress, String username) {
        failures.asMap()
                .computeIfAbsent(key(clientAddress, username), k -> new AtomicInteger())
                .incrementAndGet();
    }

    public void recordSuccess(String clientAddress, String username) {
        failures.invalidate(key(clientAddress, username));
    }

    private static String key(String clientAddress, String username) {
        return clientAddress + "|" + (username == null ? "" : username.trim().toLowerCase(Locale.ROOT));
    }
}
