package com.rabbit.common.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data // generates getters, setters, toString, equals, hashCode
@AllArgsConstructor // generates constructor with all fields
public class UserExampleDto implements JsonSerializable {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private int id;
    private String name;
    private int age;

    @Override
    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this); // serialize to JSON
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }
}