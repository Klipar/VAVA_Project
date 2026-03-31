package com.rabbit.server.repository;

import com.rabbit.common.dto.NotificationDto;
import com.rabbit.server.service.DatabaseService;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;


import java.util.List;
import java.util.Map;

public class NotificationRepository {
    private final DatabaseService db;

    public NotificationRepository(DatabaseService db) {
        this.db = db;
    }

    public Optional<NotificationDto> findMessageById(Long message_id) throws SQLException {
        String query = "SELECT * FROM notification WHERE id = ?";
        try {
            List<Map<String, Object>> results = db.query(query, message_id);

            if (results.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(mapToNotificationDto(results.get(0)));
        } catch (SQLException e) {
            throw new RuntimeException("Could not find notification with id", e);
        }
    }

    public List<NotificationDto> findAll() throws SQLException {
        String query = "SELECT * FROM notification";
        try {
            List<Map<String, Object>> results = db.query(query);
            List<NotificationDto> notifications = new ArrayList<>();

            for (Map<String, Object> row : results) {
                notifications.add(mapToNotificationDto(row));
            }

            return notifications;
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve notifications", e);
        }
    }

    public Long save(NotificationDto notDto) {
        String sql = "INSERT INTO notification (message, created_at) VALUES (?, ?)";

        try {
            return db.insertAndGetId(
                    sql,
                    notDto.getMessage(),
                    notDto.getCreated_at()
            );
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message ", e);
        }
    }

    private NotificationDto mapToNotificationDto(Map<String, Object> row) {
        Long id = ((Number) row.get("id")).longValue();
        String message = (String) row.get("message");

        LocalDateTime created_at = null;
        Timestamp timestamp = (Timestamp) row.get("created_at");
        if (timestamp != null) {
            created_at = timestamp.toLocalDateTime();
        }

        return new NotificationDto(id, message, created_at);
    }
}

class Main {
    public static void main(String[] args) throws SQLException {
        DatabaseService dbService = DatabaseService.getInstance();
        NotificationRepository repo = new NotificationRepository(dbService);

        NotificationDto dto = new NotificationDto(
                null,
                "Succesful notification test",
                LocalDateTime.now()
        );

        Long insertedId = repo.save(dto);
        System.out.println("Inserted ID: " + insertedId);

        Optional<NotificationDto> result = repo.findMessageById(2L);

        List<NotificationDto> allNotifications = repo.findAll();
        for (NotificationDto n : allNotifications) {
            System.out.println(n.getId() + ": " + n.getMessage() + " at " + n.getCreated_at());
        }
    }
}

