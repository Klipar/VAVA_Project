package com.rabbit.common.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto implements JsonSerializable {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private Long id;
    private String message;
    private LocalDateTime created_at;
    private Boolean isRead;

    @Override
    public String toJson() {
       try {
           return OBJECT_MAPPER.writeValueAsString(this);
       } catch (Exception e) {
           throw new RuntimeException("Failed to get JSON serialization", e);
       }
    }
}
