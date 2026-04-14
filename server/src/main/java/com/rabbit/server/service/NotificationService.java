package com.rabbit.server.service;

import com.rabbit.common.dto.NotificationDto;
import com.rabbit.server.repository.NotificationRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class NotificationService {
    private final NotificationRepository repo = new NotificationRepository(DatabaseService.getInstance());

    public List<NotificationDto> getNotificationsForUser(int userId) throws SQLException {
        return repo.findByUserId(userId);
    }

    public boolean markAsRead(int userId, long notificationId) throws SQLException {
        Optional<NotificationDto> existing = repo.findById(notificationId);
        if (existing.isEmpty()) return false;
        return repo.markAsRead(userId, notificationId);
    }

    public NotificationDto createNotification(int userId, NotificationDto dto) throws SQLException {
        dto.setCreated_at(LocalDateTime.now());
        long notificationId = repo.save(dto);
        repo.linkToUser(notificationId, userId);
        return repo.findById(notificationId).orElseThrow(() -> new SQLException("Failed to retrieve created notification"));
    }
}
