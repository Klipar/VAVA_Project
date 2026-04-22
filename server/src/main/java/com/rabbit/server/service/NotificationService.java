package com.rabbit.server.service;

import com.rabbit.common.dto.NotificationDto;
import com.rabbit.server.repository.NotificationRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class NotificationService {
    private static final NotificationRepository repo = new NotificationRepository(DatabaseService.getInstance());

    public List<NotificationDto> getNotificationsForUser(int userId) throws SQLException {
        return repo.findByUserId(userId);
    }

    public boolean markAsRead(int userId, long notificationId) throws SQLException {
        Optional<NotificationDto> existing = repo.findById(notificationId);
        if (existing.isEmpty()) return false;
        return repo.markAsRead(userId, notificationId);
    }

    public static void sendSystemNotification(int usedId, String message) {
        try {
            NotificationDto dto = new NotificationDto();
            dto.setMessage(message);
            dto.setCreated_at(LocalDateTime.now());
            dto.setIsRead(false);
            dto.setId(0L);

            long notificationId = repo.save(dto);
            repo.linkToUser(notificationId,usedId);
            System.out.println("System notification sent to" +usedId + ": " + message);
        }catch (SQLException e){
            e.printStackTrace();
        }
    }
}
