package com.rabbit.client;

import java.util.Locale;
import java.util.ResourceBundle;

import com.rabbit.common.dto.UserDto;

public class Config {
    private UserDto user = null;
    private ResourceBundle bundle = null;
    private Locale currentLocale = null;
    private String token = null;
    private static final String BASE_URL = "http://localhost:6969";

    private Config() {
        Locale.setDefault(Locale.US);
    }

    private static class Holder {
        private static final Config INSTANCE = new Config();
    }

    public static Config getInstance() {
        return Holder.INSTANCE;
    }

    public String getBaseUrl() {
        return BASE_URL;
    }

    public UserDto getUser() {
        return user;
    }

    public ResourceBundle getBundle() {
        return bundle;
    }

    public Locale getCurrentLocale() {
        return currentLocale;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setUser(UserDto user) {
        this.user = user;
        this.currentLocale = Locale.of("en"); // TODO: in future extract locale from user
        // this.currentLocale = Locale.of("sk");

        bundle = ResourceBundle.getBundle(
            "com.rabbit.client.localization.messages",
            currentLocale
        );
    }

    public void setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
    }
}