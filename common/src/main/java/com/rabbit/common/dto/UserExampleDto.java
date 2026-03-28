package com.rabbit.common.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UserExampleDto implements JsonSerializable {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String name;
    private int age;
    private int id;

    public UserExampleDto(int id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    @Override
    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this); // serialize to JSON
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    public int getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
}