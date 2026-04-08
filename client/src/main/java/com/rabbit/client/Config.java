package com.rabbit.client;

import com.rabbit.common.dto.UserDto;

public class Config {
    private UserDto user = null;


    private Config() {}

    private static class Holder {
        private static final Config INSTANCE = new Config();
    }

    public static Config getInstance() {
        return Holder.INSTANCE;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }
}
