package com.rabbit.client.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class SessionStorage {
    private static final String SESSION_FILE = ".rabbit_client_session.json";
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static Path getSessionPath() {
        return Paths.get(System.getProperty("user.home"), SESSION_FILE);
    }

    public static void saveSession(String token, Long userId, String userEmail) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("auth_token", token);
            data.put("user_id", String.valueOf(userId));
            data.put("user_email", userEmail);
            mapper.writeValue(getSessionPath().toFile(), data);
        } catch (IOException e) {
            System.err.println("Failed to save session: " + e.getMessage());
        }
    }

    public static Map<String, String> loadSession() {
        try {
            Path path = getSessionPath();
            if (Files.exists(path)) {
                return mapper.readValue(path.toFile(), new TypeReference<>() {});
            }
        } catch (IOException e) {
            System.err.println("Failed to load session: " + e.getMessage());
        }
        return null;
    }

    public static void clearSession() {
        try {
            Path path = getSessionPath();
            if (Files.exists(path)) {
                Files.delete(path);
            }
        } catch (IOException e) {
            System.err.println("Failed to clear session: " + e.getMessage());
        }
    }
}