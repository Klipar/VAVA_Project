package com.rabbit.client.model;


import com.rabbit.common.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {
    private String token;
    private UserDto user;
    private boolean isLoggedIn;

    public UserSession(String token, UserDto user) {
        this.token = token;
        this.user = user;
        this.isLoggedIn = true;
    }

    public void clear() {
        this.token = null;
        this.user = null;
        this.isLoggedIn = false;
    }

    public boolean isValid() {
        return isLoggedIn && token != null && !token.isEmpty() && user != null;
    }
}