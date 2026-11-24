package com.quickcourt.config;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Very simple login-session store kept in memory.
 * After login the server creates a random token and remembers which user it belongs to.
 * The browser sends this token in the "Authorization: Bearer ..." header.
 * (In a bigger system you would use JWT or Redis; this is enough for an MVP.)
 */
@Component
public class TokenStore {

    private static final long VALID_SECONDS = 24 * 60 * 60; // 24 hours

    private record Session(Long userId, Instant expiresAt) {
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public String create(Long userId) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new Session(userId, Instant.now().plusSeconds(VALID_SECONDS)));
        return token;
    }

    public Optional<Long> validate(String token) {
        if (token == null) {
            return Optional.empty();
        }
        Session s = sessions.get(token);
        if (s == null) {
            return Optional.empty();
        }
        if (s.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(s.userId());
    }

    public void remove(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }
}
