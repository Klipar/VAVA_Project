package com.rabbit.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.client.Config;
import com.rabbit.client.model.UserSession;
import com.rabbit.common.dto.UserDto;
import lombok.Getter;

import java.net.http.HttpResponse;
import java.util.Map;

public class UserService {
    private static UserService instance;

    @Getter
    private UserSession currentSession;

    private ApiClient apiClient;
    private final ObjectMapper objectMapper;

    private UserService() {
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    private ApiClient getApiClient() {
        if (apiClient == null) {
            apiClient = ApiClient.getInstance();
        }
        return apiClient;
    }

    public void login(String token, UserDto user) {
        this.currentSession = new UserSession(token, user);
        SessionStorage.saveSession(token, user.getId(), user.getEmail());
    }

    public void logout() {
        if (currentSession != null) {
            currentSession.clear();
            SessionStorage.clearSession();
        }
    }

    public boolean isLoggedIn() {
        return currentSession != null && currentSession.isValid();
    }

    public String getToken() {
        return isLoggedIn() ? currentSession.getToken() : null;
    }

    public UserDto getCurrentUser() {
        return isLoggedIn() ? currentSession.getUser() : null;
    }

    public void loadSavedSession() {
        Map<String, String> sessionData = SessionStorage.loadSession();

        if (sessionData != null) {
            String token = sessionData.get("auth_token");
            String userId = sessionData.get("user_id");

            if (token != null && userId != null) {
                try {
                    HttpResponse<String> response = getApiClient().getAuthenticated("/users/" + userId, token);

                    if (getApiClient().isSuccess(response)) {
                        UserDto user = objectMapper.readValue(response.body(), UserDto.class);
                        this.currentSession = new UserSession(token, user);
                        Config.getInstance().setToken(token);
                        Config.getInstance().setUser(user);
                    } else {
                        SessionStorage.clearSession();
                    }
                } catch (Exception e) {
                    SessionStorage.clearSession();
                }
            }
        }
    }
}