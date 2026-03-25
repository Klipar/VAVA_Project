package com.rabbit.common.dto;

import java.io.Serializable;

/**
 * Test DTO class for sharing between modules.
 */
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;
    private String email;
    private int age;

    public UserDTO() {
    }

    public UserDTO(String username, String email, int age) {
        this.username = username;
        this.email = email;
        this.age = age;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "UserDTO{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", age=" + age +
                '}';
    }
}