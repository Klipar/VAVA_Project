package com.rabbit.server.middleware;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthMiddleware {
    private static volatile AuthMiddleware instanse;
    private static final int TOKEN_TTL_SECONDS = 3600;
    private static final Object LOCK = new Object();

    private ConcurrentHashMap<String, TokenEntry> tokens = new ConcurrentHashMap<>();

    private static class TokenEntry {
        final int userId;
        volatile long lastAccessedAt;

        TokenEntry(int userId) {
            this.userId = userId;
            this.lastAccessedAt = System.currentTimeMillis();
        }
    }

    private AuthMiddleware() {}

    public static AuthMiddleware getInstanse() {
        if (instanse == null) {
            synchronized (LOCK) {
                instanse = new AuthMiddleware();
            }
        }
        return instanse;
    }

    public String createToken(int userId) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, new TokenEntry(userId));
        return token;
    }

    // The method checks if the token is valid and returns the user ID. If the token is valid ttl is reseted.
    public Integer getUserId(String token) {
        TokenEntry entry = tokens.get(token);
        if (entry == null) {
            return null;
        }

        long elapsedSeconds = (System.currentTimeMillis() - entry.lastAccessedAt) / 1000;
        if (elapsedSeconds > TOKEN_TTL_SECONDS) {
            tokens.remove(token);
            return null;
        }

        entry.lastAccessedAt = System.currentTimeMillis();
        return entry.userId;
    }
}
