package com.rabbit.server.repository;

import com.rabbit.common.dto.NotificationDto;
import com.rabbit.server.service.DatabaseService;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class NotificationRepository {
    private final DatabaseService db;

    public NotificationRepository(DatabaseService db) {
        this.db = db;
    }

    public List<NotificationDto> findByUserId(int userId) throws SQLException {
        return db.query("""
                SELECT n.*, un.is_read
                FROM notification n
                JOIN user_notification un ON n.id = un.notification_id
                WHERE un.user_id = ?
                ORDER BY n.created_at DESC
                """, userId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public Optional<NotificationDto> findById(long notificationId) throws SQLException {
        return db.query("SELECT * FROM notification WHERE id = ?", notificationId)
                .stream()
                .map(this::mapToDto)
                .findFirst();
    }

    public Long save(NotificationDto dto) throws SQLException {
        return db.insertAndGetId(
                "INSERT INTO notification (message, created_at) VALUES (?, ?)",
                dto.getMessage(),
                dto.getCreated_at()
        );
    }

    public void linkToUser(long notificationId, int userId) throws SQLException {
        db.update(
                "INSERT INTO user_notification (user_id, notification_id) VALUES (?, ?)",
                userId, notificationId
        );
    }

    public boolean markAsRead(int userId, long notificationId) throws SQLException {
        return db.update(
                "UPDATE user_notification SET is_read = true WHERE user_id = ? AND notification_id = ?",
                userId, notificationId
        ) > 0;
    }

    private NotificationDto mapToDto(Map<String, Object> row) {
        NotificationDto dto = new NotificationDto();
        dto.setId(((Number) row.get("id")).longValue());
        dto.setMessage((String) row.get("message"));
        Timestamp ts = (Timestamp) row.get("created_at");
        if (ts != null) dto.setCreated_at(ts.toLocalDateTime());
        if (row.containsKey("is_read")) dto.setRead((Boolean) row.get("is_read"));
        return dto;
    }
}
