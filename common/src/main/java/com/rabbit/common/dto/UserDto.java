package com.rabbit.common.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.common.enums.UserRole;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for user information.
 * ID can only be set during object creation (no setter).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto implements JsonSerializable {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private Long id;                     // unique user identifier (no setter - only set via constructor)
    private String name;                 // real name
    private String nickname;             // display name (alias)
    private String email;                // email address (used as login)
    private UserRole role;               // global role (MANAGER, TEAM_LEADER, WORKER)
    private LocalDateTime createdAt;     // account creation timestamp

    /**
     * Custom setter for id to prevent modification after creation.
     * This method exists only for JSON deserialization but should not be used manually.
     */
    public void setId(Long id) {
        if (this.id == null) {
            this.id = id;
        } else {
            throw new IllegalStateException("ID cannot be changed once set");
        }
    }

    @Override
    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize UserDto to JSON", e);
        }
    }

    public static UserDto fromJson(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, UserDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize UserDto from JSON", e);
        }
    }
}