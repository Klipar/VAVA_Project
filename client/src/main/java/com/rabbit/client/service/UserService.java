package com.rabbit.client.service;

import com.rabbit.client.model.UserSession;
import com.rabbit.common.dto.UserDto;
import lombok.Getter;

public class UserService {
    private static UserService instance;

    @Getter
    private UserSession currentSession;

    private UserService() {}

    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    public void login(String token, UserDto user) {
        this.currentSession = new UserSession(token, user);
        saveToLocalStorage();
    }

    public void logout() {
        if (currentSession != null) {
            currentSession.clear();
            clearLocalStorage();
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

    private void saveToLocalStorage() {
        try {
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(UserService.class);
            prefs.put("auth_token", currentSession.getToken());
            prefs.put("user_id", String.valueOf(currentSession.getUser().getId()));
            prefs.put("user_email", currentSession.getUser().getEmail());
        } catch (Exception e) {
            System.err.println("Failed to save session: " + e.getMessage());
        }
    }

    private void clearLocalStorage() {
        try {
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(UserService.class);
            prefs.remove("auth_token");
            prefs.remove("user_id");
            prefs.remove("user_email");
        } catch (Exception e) {
            System.err.println("Failed to clear session: " + e.getMessage());
        }
    }
    // TODO: use that method to load session from local storage
    public void loadSavedSession() {
        try {
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(UserService.class);
            String token = prefs.get("auth_token", null);
            String userId = prefs.get("user_id", null);
            String userEmail = prefs.get("user_email", null);

            if (token != null && userId != null) {
                UserDto user = new UserDto();
                user.setId(Long.parseLong(userId));
                user.setEmail(userEmail);
                this.currentSession = new UserSession(token, user);
            }
        } catch (Exception e) {
            System.err.println("Failed to load session: " + e.getMessage());
        }
    }
}